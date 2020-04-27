(ns checkout.ui.return-link
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private return-link-back-link-molecule
  [{:return-link.back-link/keys [id label target]}]
  (when (and id label target)
    [:div.proxima.title-3.shout.bold.flex
     [:a.inherit-color
      (merge {:data-test id}
             (apply utils/route-back-or-to
                    (cons nil target)))
      [:div.flex.line-height-1
       (ui/back-caret {})
       [:span.ml1.border-bottom.border-black.border-width-3
        label]]]]))

(defn ^:private return-link-link-molecule
  [{:return-link.link/keys [id label target]}]
  (when (and id label target)
    [:div.proxima.title-3.shout.bold.flex
     [:a.inherit-color
      (merge {:data-test id}
             (apply utils/fake-href target))
      [:div.flex.line-height-1
       (ui/back-caret {})
       [:span.ml1.border-bottom.border-black.border-width-3
        label]]]]))

(c/defcomponent organism
  [data _ _]
  [:div.border-bottom.border-gray.border-width-1.m-auto.col-7-on-dt
   [:div.px2.my2
    (return-link-link-molecule data)
    (return-link-back-link-molecule data)]])
