(ns storefront.component)

;; clj
#?(:clj
   (defmacro -create [platform body]
     (if (= platform :cljs)
       `(om.core/component (sablono.core/html (~body)))
       `(~body))))

;; cljc
(defn blah [body-fn]
   (-create #?(:clj :clj :cljs :cljs) body-fn))

;; clj
#?(:clj
   (defmacro create [body]
     `(~'storefront.component/blah (fn [] ~@body))))

#?(:clj (defmacro build [component data opts]
          #_(:clj (component data nil (:opts opts))
             :cljs (om.core/build component data opts))))
