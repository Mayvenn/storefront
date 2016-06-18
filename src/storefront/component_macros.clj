(ns storefront.component-macros)

(defmacro create [body]
  `(om.core/component (~'sablono.core/html ~body)))

(defmacro build [component data opts]
  `(om.core/build ~component ~data ~opts))
