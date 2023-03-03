(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :as set]
             [spice.selector :as selector]
             [spice.maps :as maps]
             [clojure.walk :as walk]))

(def content-hierarchy
  (-> (make-hierarchy)
      (derive :content/heading-1 :content/heading)
      (derive :content/heading-2 :content/heading)
      (derive :content/heading-3 :content/heading)
      (derive :content/heading-4 :content/heading)
      (derive :content/heading-5 :content/heading)
      (derive :content/heading-6 :content/heading)))

(defmulti build-hiccup-tag
  (fn [{:as node
        :keys [content data node-type]}]
    (keyword "content" node-type))
  :hierarchy #'content-hierarchy)

(defn build-hiccup-content [content data]
  (into []
        (map build-hiccup-tag)
        content))

(defmethod build-hiccup-tag :content/heading [{:as _node
                                          :keys [content data]}]
  [:h3 (build-hiccup-content content data)])

(defmethod build-hiccup-tag :content/paragraph [{:as _node
                                            :keys [content data]}]
  [:p (build-hiccup-content content data)])

(defmethod build-hiccup-tag :content/text [{:as _node
                                       :keys [value]}]
  ;; TODO: Strip initial newlines?
  value)

(defmethod build-hiccup-tag :default [{:as node
                                  :keys [content data]}]
  ;; TODO: Productionalize this error message
  (println "Attempting to render unknown node type" node)
  nil)

(defn render-selected-value [selected-value]
  (build-hiccup-content (:content (:value selected-value))
                        (:data (:value selected-value))))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  #?(:cljs
     (do
       (->> {:product-overview-description :pdp.details.overview/description
             ;:pdp/colorable                :pdp.details.care/can-it-be-colored?
             ;:pdp/in-store-services        :pdp.details.customize-your-wig/in-store-services
             ;:pdp/maintenance-level        :pdp.details.care/maintenance-level
             }
            (set/rename-keys cms-dynamic-content-data)
            (maps/map-values
             (fn [template-slot]
               (->> (template-slot :selectable-values)
                    (map (fn [selectable-value ]
                           (merge selectable-value
                                  (:selector selectable-value))))
                    (selector/match-essentials
                     (assoc sku :selector/essentials (into #{} (comp
                                                                (map :selector)
                                                                (mapcat keys))
                                                           (:selectable-values template-slot))))
                    ;; TODO Move the code responsible for merging the selector attributes into the top level
                    ;; of the selectable value to the API / Handler areas)
                    first
                    render-selected-value)))))))

;; TODO: Remap keys from contentful to our keys
