(ns storefront.component)

(defmacro create [body]
  `(om.core/component (~'sablono.core/html ~body)))

(defmacro build [component data opts]
  `(om.core/build ~component ~data ~opts))

(defmacro html [content]
  `(~'sablono.core/html ~content))
