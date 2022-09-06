(ns homepage.ui.blog
  (:require [clojure.string :refer [join split]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent organism
  [{:blog/keys [id target heading ucare-id date read-time beginning author] } _ _]
  (when heading
    [:div.mx-auto.flex-on-tb-dt.col-10
     {:key (str "blog-" id)}
     [:a
      (merge (apply utils/route-to target)
             {:data-test  (str "to-" id)
              :aria-label heading})
      (ui/defer-ucare-img {:class      "block col-12"
                        :smart-crop "600x400"
                        :alt        ""}
        ucare-id)]
     [:div.p3.col-9-on-tb-dt
      [:h2.canela.title-2.mb2 heading]
      [:div.flex
       [:div.content-3.shout.mr2 author]
       [:div.dark-dark-gray.content-3.shout date " • " read-time]]
      [:div.content-4.py3 beginning]
      [:div.shout.col-8 (ui/button-medium-primary
                   (merge
                    (apply utils/route-to target)
                    {:data-test (str "go-to-" id)})
                   "Read More")]]]))
