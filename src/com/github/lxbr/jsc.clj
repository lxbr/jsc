(ns com.github.lxbr.jsc
  (:require [com.github.lxbr.jsc.util :as util]))

(set! *warn-on-reflection* true)

(util/create-bindings!
 {:lib-path             "/System/Library/Frameworks/JavaScriptCore.framework/JavaScriptCore"
  :headers-path         "/System/Library/Frameworks/JavaScriptCore.framework/Headers"
  :precompile-functions false})

(util/create-closure-builders!
 [{:name "JSObjectCallAsFunctionCallback"
   :return {:kind :pointer}
   :params [{:kind :pointer
             :name "ctx"}
            {:kind :pointer
             :name "function"}
            {:kind :pointer
             :name "thisObject"}
            {:kind :int32
             :name "argumentCount"}
            {:kind :int64 :pointer true
             :name "arguments"}
            {:kind :int64 :pointer true
             :name "exception"}]}])

(comment

  ;; callback from JSC -> JVM
  (let [ctx (JSGlobalContextCreate 0)
        cb  (.getAddress (JSObjectCallAsFunctionCallback
                          (reify com.kenai.jffi.Closure
                            (invoke [this buffer]
                              (prn :callback)))))
        f   (JSObjectMakeFunctionWithCallback ctx 0 cb)]
    (JSObjectCallAsFunction ctx f 0 0 (long-array 0) (long-array 0)))

  ;; eval JS
  (let [script     "[1,2,3].reduce((x, y) => x + y, 0)"
        ctx        (JSGlobalContextCreate 0)
        script-ref (JSStringCreateWithUTF8CString (.getBytes script "UTF-8"))
        result     (JSEvaluateScript ctx script-ref 0 0 0 (long-array 0))]
    (JSValueToNumber ctx result (long-array 0)))

  )
