(ns adventure.faq
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.accordion :as accordion]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn free-install-query
  [data]
  (when-let [faq (get-in data (conj keypaths/cms-faq :free-mayvenn-services))]
    {:expanded-index (get-in data keypaths/faq-expanded-section)
     :sections       (for [{:keys [question answer]} (:question-answers faq)]
                       {:title   (:text question)
                        :content answer})}))

(defn component [{:keys [expanded-index sections background-color]}]
  [:div.px6.mx-auto.col-10-on-dt.py6
   {:class background-color}
   [:div.canela.title-1.center.my7 "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         (map
                        (fn [{:keys [title content]}]
                          {:title [:content-1 title]
                           :content content})
                        sections)}
    {:opts {:section-click-event events/faq-section-selected}})])
