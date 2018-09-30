# jsc

Clojure bindings for the JavaScriptCore engine on macos.

## Example Usage

```clojure
;; at the REPL
(require 'com.github.lxbr.jsc)

(in-ns 'com.github.lxbr.jsc)

;; callback from JSC -> JVM
(let [ctx (JSGlobalContextCreate 0)
      cb  (.getAddress (JSObjectCallAsFunctionCallback
                        (reify com.kenai.jffi.Closure
                          (invoke [this buffer]
                            (prn :callback)))))
      f   (JSObjectMakeFunctionWithCallback ctx 0 cb)]
  (JSObjectCallAsFunction ctx f 0 0 (long-array 0) (long-array 0)))
;; prints `:callback`

;; eval JS
(let [script     "[1,2,3].reduce((x, y) => x + y, 0)"
      ctx        (JSGlobalContextCreate 0)
      script-ref (JSStringCreateWithUTF8CString (.getBytes script "UTF-8"))
      result     (JSEvaluateScript ctx script-ref 0 0 0 (long-array 0))]
  (JSValueToNumber ctx result (long-array 0)))
;; => 6.0
```

## API Description

The API closely follows the one specified in the JavaScriptCore C headers.
A data representation can be created via:

```clojure
;; at the REPL
(require 'com.github.lxbr.jsc.util)

(in-ns 'com.github.lxbr.jsc.util)

(->> (read-function-declarations)
     (mapv jsc-to-cffi-types)
     (clojure.pprint/pprint))
```

## License

This project is licensed under the terms of the Eclipse Public License 1.0 (EPL).


