(ns stylist-profile.ui-v2021-10.sticky-select-stylist
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn sticky-select-stylist-cta-molecule
  [{:sticky-select-stylist.cta/keys [id label target]}]
  (when (and id label target)
    [:div.col-12
     (ui/button-medium-primary
      (merge {:data-test id}
             (apply utils/fake-href target))
      [:div.flex.items-center.justify-center.inherit-color.button-font-2 label])]))

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
                      ;; HACK: Do two observers need to exist??
                      page_cta_1 (.querySelector js/document "#nonsticky-select-stylist-1")
                      page_cta_2 (.querySelector js/document "#nonsticky-select-stylist-2")]
                  (.observe observer page_cta_1)
                  (.observe observer page_cta_2))))
  #?(:cljs
     (will-unmount [this]
                   (let [observer (-> this c/get-state :observer)
                         page_cta_1 (.querySelector js/document "#nonsticky-select-stylist-1")
                         page_cta_2 (.querySelector js/document "#nonsticky-select-stylist-2")]
                     (when (and observer (or page_cta_1 page_cta_2))
                       (.unobserve observer page_cta_1)
                       (.unobserve observer page_cta_2)
                       (.disconnect observer)))))

  (render [this]
          (let [state (c/get-state this)
                data  (c/get-props this)]
            (c/html
             [:div
              [:div.hide-on-mb
               [:div.bg-pale-purple.fixed.border-none.bottom-0.py3.left-0.right-0
                {:class (if-not (:hide-sticky? state) "slide-up" "slide-down")}
                [:div.flex.mx-auto.col-3.justify-center.my4
                 (sticky-select-stylist-cta-molecule
                  (update data :sticky-select-stylist.cta/id str "-desktop"))]]]

              [:div.hide-on-tb-dt
               [:div.bg-pale-purple.col-12.fixed.center.mtj3
                {:class (if-not (:hide-sticky? state) "slide-up" "slide-down")}
                [:div.flex.mx-auto.col-10.justify-center.my4
                 (sticky-select-stylist-cta-molecule data)]]]]))))
