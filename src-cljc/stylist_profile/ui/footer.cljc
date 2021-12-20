(ns stylist-profile.ui.footer
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn footer-body-molecule
  [{:footer.body/keys [copy id]}]
  (c/html
   (when id
     [:div.h4.pt3
      {:data-test id}
      copy])))

(defn footer-cta-molecule
  [{:footer.cta/keys [id label] [event :as target] :footer.cta/target}]
  (c/html
   (when id
     [:div
      [:div.col-2.mx-auto.pt2.hide-on-mb
       (ui/button-small-secondary
        (merge {:data-test id}
               (if (= :navigate (first event))
                 (apply utils/route-to target)
                 (apply utils/fake-href target)))
        label)]
      [:div.col-5.mx-auto.pt2.hide-on-tb-dt
       (ui/button-small-secondary
        (merge {:data-test id}
               (if (= :navigate (first event))
                 (apply utils/route-to target)
                 (apply utils/fake-href target)))
        label)]])))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:footer.border-top.border-cool-gray.border-width-2.center
     (footer-body-molecule data)
     (footer-cta-molecule data)
     ;; Space for the fixed button on mobile and dt variations
     ;; Not loving this, would like to know where the responsibility lies
     ;; for this "spacer" in the design system
     [:div.pyj3.hide-on-mb ui/nbsp]
     [:div.myj1.pyj3.hide-on-tb-dt ui/nbsp]]))
