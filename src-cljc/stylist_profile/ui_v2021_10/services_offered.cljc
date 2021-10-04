(ns stylist-profile.ui-v2021-10.services-offered
  (:require [storefront.component :as c]))

(defn services-title-molecule
  [{:title/keys [id primary]}]
  (c/html
   [:div.title-3.proxima.shout
    {:data-test id
     :key       id}
    primary]))

(c/defcomponent service-pill-molecule
  [{:keys [title]} _ {:keys [id]}]
  [:span.capped.bg-pale-purple.px2.py1.mr2.content-3
   {:key id}
   title])

(c/defcomponent organism
  [data _ _]
  [:div.pt5
   (services-title-molecule data)
   (c/elements service-pill-molecule data :services)])
