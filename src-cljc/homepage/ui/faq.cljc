(ns homepage.ui.faq
  (:require [clojure.string :refer [join]]
            [storefront.component :as c]
            [storefront.components.accordion :as accordion]
            [storefront.events :as e]))

(defn ld+json
  [sections]
  (let [entries (->> sections
                     (map (fn [{:faq/keys [title content]}]
                            (let [ps (->> content
                                          first
                                          :paragraph
                                          (map :text)
                                          (apply str))]
                              (str "{"
                                   "\"@type\": \"Question\","
                                   "\"name\": \"" title "\","
                                   "\"acceptedAnswer\": {"
                                   "\"@type\": \"Answer\","
                                   "\"text\": \"" ps "\""
                                   "}"
                                   "}"))))
                     (join ","))]
    (str "{"
         "\"@context\": \"https://schema.org\","
         "\"@type\": \"FAQPage\","
         "\"mainEntity\": ["
         entries
         "]"
         "}")))

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
      [:script {:type                    "application/ld+json"
                :dangerouslySetInnerHTML {:__html (ld+json sections)}}]
      (c/build accordion/component
               {:expanded-indices #{expanded-index}
                :sections         (mapv
                                   (fn [{:faq/keys [title content]}]
                                     {:title   [:content-1 title]
                                      :content content})
                                   sections)}
               {:opts {:section-click-event e/faq-section-selected}})]]))
