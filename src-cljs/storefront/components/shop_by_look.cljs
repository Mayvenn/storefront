(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths] ))

(defn inner-component [{:keys [container-id pixlee-loaded?]} owner opts]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/shop-by-look-component-mounted {:container-id container-id}))

    om/IWillUnmount
    (will-unmount [this]
      (handle-message events/shop-by-look-component-unmounted))

    om/IRender
    (render [this]
      (html
       [:div
        [:div.center.bg-white.py3
         [:div.h2.navy "Shop by look"]
         [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
          {:style {:width "101px" :height "85px"}} ]
         [:p.dark-gray.col-10.md-up-col-6.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easy add it to your bag!"]]
        [:div {:id container-id}]]))))

(defn component [{:keys [pixlee-loaded?] :as data} owner opts]
  (om/component
   (html
    (when pixlee-loaded?
      (om/build inner-component data opts)))))

(defn query [data]
  {:container-id   "pixlee-container"
   :pixlee-loaded? (get-in data keypaths/loaded-pixlee)})

(defn built-component [data opts]
  (om/build component (query data) opts))
