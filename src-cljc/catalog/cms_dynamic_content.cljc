(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :as set]
             [spice.selector :as selector]
             [spice.maps :as maps]
             [clojure.walk :as walk]))

(def rich-text-hierarchy
  (-> (make-hierarchy)
      ;; NOTE: This basically is just setting it up so that the keyword on the left gets built as the entity on the right
      ;;      child              parent
      (derive :rich-text/heading-1 :rich-text/heading)
      (derive :rich-text/heading-2 :rich-text/heading)
      (derive :rich-text/heading-3 :rich-text/heading)
      (derive :rich-text/heading-4 :rich-text/heading)
      (derive :rich-text/heading-5 :rich-text/heading)
      (derive :rich-text/heading-6 :rich-text/heading)))

(defmulti build-hiccup-tag
  (fn [{:as _node
        :keys [node-type]}]
    (keyword "rich-text" node-type))
  :hierarchy #'rich-text-hierarchy)

(defn build-hiccup-content [tag content]
  (into tag
        (map build-hiccup-tag)
        content))

(defmethod build-hiccup-tag :rich-text/heading [{:keys [content]}]
  (build-hiccup-content [:h3] content))

(defmethod build-hiccup-tag :rich-text/paragraph [{:keys [content]}]
  (build-hiccup-content [:p] content))

(defmethod build-hiccup-tag :rich-text/hyperlink [{:keys [content data]}]
  ;; TODO: Strip initial newlines?
  (build-hiccup-content [:a {:href (:uri data)}] content))

(defmethod build-hiccup-tag :rich-text/text [{:keys [value]}]
  ;; TODO: Strip initial newlines?
  value)

(defmethod build-hiccup-tag :default [node]
  ;; TODO: Productionalize this error message
  (println "Attempting to render unknown node type" node)
  nil)

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
                    :value
                    :content
                    (into [:div]
                          (map build-hiccup-tag)))))))))

;; TODO: Remap keys from contentful to our keys
