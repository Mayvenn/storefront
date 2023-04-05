(ns homepage.ui-v2022-09
  (:require [storefront.component :as c]
            [adventure.components.layered :as layered]))

(c/defcomponent template
  [data _ _]
  [:div
   (c/build layered/component data nil)])
