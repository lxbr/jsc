(ns com.github.lxbr.jsc.util
  (:require [clojure.java.io :as io]
            [com.github.lxbr.effing :as ffi])
  (:import com.kenai.jffi.Library))

(set! *warn-on-reflection* true)

(defn parse-params
  [params]
  (->> (clojure.string/split params #"\s*,\s*")
       (eduction               
        (map #(clojure.string/split % #"\s+"))
        (map (fn [[a b c]]
               (let [{:keys [name kind] :as param}
                     (cond
                       (nil? b) {:kind a :name (str (gensym "arg"))}
                       (= a "const")
                       {:const true
                        :kind  b
                        :name  (or c (str (gensym "arg")))}
                       :else
                       {:kind a
                        :name (or b (str (gensym "arg")))})]
                 (cond-> param
                   (and (some? name)
                        (clojure.string/ends-with? name "[]"))
                   (-> (update :name #(clojure.string/replace % "[]" ""))
                       (assoc :pointer true))
                   (and (some? name)
                        (clojure.string/starts-with? name "*"))
                   (-> (update :name subs 1)
                       (assoc :pointer true))
                   (clojure.string/ends-with? kind "*")
                   (-> (update :kind subs 0 (dec (count kind)))
                       (assoc :pointer true)))))))))

(defn parse-header
  [header]
  (->> (re-seq #"JS_EXPORT\s+(\w+)\s+(\w+)\(([^)]*)\)" header)
       (eduction
        (map (fn [[_ return name params]]
               {:return {:kind return}
                :name   name
                :params (into [] (parse-params params))})))))

(defn read-function-declarations
  []
  (->> (slurp (io/resource "JavaScript.h"))
       (re-seq #"#include <JavaScriptCore/(.*)>")
       (eduction
        (map second)
        (map (comp slurp io/resource))
        (mapcat parse-header))))

(def type-map
  {"JSValueRef"                        :pointer
   "JSObjectRef"                       :pointer
   "JSStringRef"                       :pointer
   "JSContextRef"                      :pointer
   "JSContextGroupRef"                 :pointer
   "JSGlobalContextRef"                :pointer
   "JSClassRef"                        :pointer
   "JSObjectCallAsFunctionCallback"    :pointer
   "JSObjectCallAsConstructorCallback" :pointer
   "JSPropertyNameArrayRef"            :pointer
   "JSClassDefinition"                 :pointer
   "JSPropertyNameAccumulatorRef"      :pointer
   "JSTypedArrayBytesDeallocator"      :pointer
   "JSTypedArrayType"                  :int32
   "JSPropertyAttributes"              :int32
   "void"                              :void
   "bool"                              :boolean
   "int"                               :int32
   "size_t"                            :int32
   "char"                              :int8
   "JSChar"                            :int16
   "unsigned"                          :int32
   "double"                            :double
   "JSType"                            :int32})

(defn jsc-to-ffi-types
  [fn-spec]
  (let [{:keys [return params]} fn-spec
        new-return (update return :kind type-map)
        new-params (mapv #(update % :kind type-map) params)]
    (assoc fn-spec
           :return new-return
           :params (if (and (== 1 (count new-params))
                            (not (:pointer (first new-params)))
                            (= :void (:kind (first new-params))))
                     []
                     new-params))))

(defn generate-bindings
  [functions lib]
  (cons 'do (map #(ffi/create-implementation lib % {:precompile-functions false}) functions)))

(defmacro create-bindings!
  []
  (let [fns (->> (read-function-declarations)
                 (mapv jsc-to-ffi-types))
        lib (-> (io/resource "JavaScriptCore")
                (io/file)
                (.getAbsolutePath)
                (Library/openLibrary (bit-or Library/LOCAL Library/LAZY)))]
    (generate-bindings fns lib)))

(defmacro create-closure-builders!
  [function-specs]
  (cons 'do (map ffi/create-closure-builder function-specs)))
