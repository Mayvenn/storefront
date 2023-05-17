(ns homepage.ui.blog
  (:require [clojure.string :refer [join split]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent organism
  [{:blog/keys [id target heading ucare-id date read-time beginning author] } _ _]
  (when heading
    [:div.mx-auto.flex-on-tb-dt.max-1080
     {:key (str "blog-" id)}
     [:a
      {:href      target
       :data-test  (str "to-" id)
       :aria-label heading}
      (ui/defer-ucare-img {:class      "block col-12"
                           :smart-crop "600x400"
                           :alt        ""}
        ucare-id)]
     [:div.p3.col-9-on-tb-dt
      [:h2.canela.title-1.mb2
       [:a.inherit-color {:href target} heading]]
      [:div.flex.mt2.content-3.shout
       [:div.mr2 author]
       [:div.gray-700 date " • " read-time]]
      [:div.pt4 beginning]
      [:div.shout.col-8.pt3 (ui/button-medium-primary
                             {:href      target
                              :data-test (str "go-to-" id)}
                             "Read More")]]]))
