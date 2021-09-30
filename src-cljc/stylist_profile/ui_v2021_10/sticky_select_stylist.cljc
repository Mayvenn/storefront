(ns stylist-profile.ui-v2021-10.sticky-select-stylist
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn sticky-select-stylist-cta-molecule
  [{:sticky-select-stylist.cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-medium-primary
     (merge {:data-test id}
            (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color.button-font-2 label])))

(c/defdynamic-component organism
  (constructor [this _props]
               #?(:cljs
                  {:hide-sticky? true
                   :observer     (js/IntersectionObserver.
                                  (fn [entry]
                                    (c/set-state! this :hide-sticky? (-> entry (aget 0) .-isIntersecting)))
                                  #js {:delay     100
                                       :threshold [1]})}))
  (did-mount [this]
             #?(:cljs
                (let [observer (-> this c/get-state :observer)
                      page_cta (.querySelector js/document "#nonsticky-select-stylist")]
                  (.observe observer page_cta))))
  #?(:cljs
     (will-unmount [this]
                   (let [observer (-> this c/get-state :observer)
                         page_cta (.querySelector js/document "#nonsticky-select-stylist")]
                     (when (and observer page_cta)
                       (.unobserve observer page_cta)
                       (.disconnect observer)))))

  (render [this]
          (let [state (c/get-state this)
                data  (c/get-props this)]
            (c/html
             [:div
              [:div.hide-on-mb
               [:div.bg-pale-purple.fixed.max-580.border-none.col-12.bottom-0.py3
                {:class (if-not (:hide-sticky? state) "slide-up" "slide-down")}
                [:div.flex.justify-center.my4
                 (sticky-select-stylist-cta-molecule
                  (update data :sticky-select-stylist.cta/id str "-desktop"))]]]

              [:div.hide-on-tb-dt
               [:div.bg-pale-purple.col-12.fixed.center.mtj3
                {:class (if-not (:hide-sticky? state) "slide-up" "slide-down")}
                [:div.flex.justify-center.my4
                 (sticky-select-stylist-cta-molecule data)]]]]))))
