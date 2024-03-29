(ns homepage.ui.faq
  (:require  [storefront.component :as c]
            [storefront.components.accordion :as accordion]
            [storefront.events :as e]))

(c/defcomponent organism
  [{:as        data
    :faq/keys  [expanded-index]
    :list/keys [sections]} _ _]
  (when (seq data)
    [:div.col-12.mx-auto.bg-pale-purple.px6.py8
     {:data-test "faq"}
     [:div.col-8-on-dt.mx-auto
      [:h2.canela.title-1.center.my7
       "Frequently Asked Questions"]
      (c/build accordion/component
               {:expanded-indices #{expanded-index}
                :sections         (mapv
                                   (fn [{:faq/keys [title content]}]
                                     {:title   [:content-1 title]
                                      :content content})
                                   sections)}
               {:opts {:section-click-event e/faq-section-selected}})]]))
