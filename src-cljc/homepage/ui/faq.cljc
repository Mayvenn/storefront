(ns homepage.ui.faq
  (:require [storefront.component :as c]
            [storefront.components.accordion :as accordion]
            [storefront.events :as e]))

(c/defcomponent organism
  [{:as data :faq/keys [expanded-index] :list/keys [sections]} _ _]
  (when (seq data)
    [:div.col-12.mx-auto.bg-pale-purple.p6
     [:div.col-8-on-dt.mx-auto
      [:div.canela.title-1.center.my7
       "Frequently Asked Questions"]
      (c/build accordion/component
               {:expanded-indices #{expanded-index}
                :sections         (mapv
                                   (fn [{:faq/keys [title paragraphs]}]
                                     {:title      [:content-1 title]
                                      :paragraphs paragraphs})
                                   sections)}
               {:opts {:section-click-event e/faq-section-selected}})]]))
