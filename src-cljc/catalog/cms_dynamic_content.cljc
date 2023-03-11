(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :as set]
             [spice.selector :as selector]
             [spice.maps :as maps]
             [clojure.string :as string]))

(defmulti build-hiccup-tag
  (fn [{:as   _node
        :keys [data content node-type]}]
    (cond
      (and (= node-type "embedded-asset-block")
           (some-> data
                   :target
                   :file
                   :content-type
                   (string/starts-with? "video")))
      :rich-text/embedded-video-block

      (and (= node-type "paragraph")
           (some-> content
                   first
                   :value
                   empty?))
      :rich-text/spacer

      :else
      (keyword "rich-text" node-type))))

(defn build-hiccup-content [tag content]
  (into tag
        (map build-hiccup-tag)
        content))

;; Not Currently Used
;; (defmethod build-hiccup-tag :rich-text/heading-1 [{:keys [content]}]
;;   (build-hiccup-content [:h1] content))

(defmethod build-hiccup-tag :rich-text/heading-2 [{:keys [content]}]
  (build-hiccup-content [:h2.canela.title-2] content))

(defmethod build-hiccup-tag :rich-text/heading-3 [{:keys [content]}]
  (build-hiccup-content [:h3.proxima.title-1] content))

(defmethod build-hiccup-tag :rich-text/heading-4 [{:keys [content]}]
  (build-hiccup-content [:h4.proxima.title-2] content))

(defmethod build-hiccup-tag :rich-text/heading-5 [{:keys [content]}]
  (build-hiccup-content [:h5.canela.title-3] content))

(defmethod build-hiccup-tag :rich-text/heading-6 [{:keys [content]}]
  (build-hiccup-content [:h6.proxima.title-3] content))

(defmethod build-hiccup-tag :rich-text/paragraph [{:keys [content]}]
  (build-hiccup-content [:div] content))

(defmethod build-hiccup-tag :rich-text/spacer [{:keys [content]}]
  (build-hiccup-content [:div.py4] content))

(defmethod build-hiccup-tag :rich-text/hyperlink [{:keys [content data]}]
  (build-hiccup-content [:a {:href (:uri data)}] content))

(defmethod build-hiccup-tag :rich-text/asset-hyperlink [{:as _node :keys [content data]}]
  (let [content-nodes (if (empty? content)
                        [{:value (:title (:target data))
                          :node-type "text"}]
                        content)]
    (build-hiccup-content [:a {:href (:url (:file (:target data)))}] content-nodes)))

(defmethod build-hiccup-tag :rich-text/unordered-list [{:keys [content]}]
  (build-hiccup-content [:ul] content))

;;TODO Needs styling before we enable this in production
(defmethod build-hiccup-tag :rich-text/ordered-list [{:keys [content]}]
  (build-hiccup-content [:ol] content))

(defmethod build-hiccup-tag :rich-text/list-item [{:keys [content]}]
  (build-hiccup-content [:li] content))

(defmethod build-hiccup-tag :rich-text/embedded-asset-block [{:as _node :keys [data]}]
  [:a {:href (:url (:file (:target data)))}
   (:title (:target data))])

(defmethod build-hiccup-tag :rich-text/embedded-video-block [{:as _node :keys [data]}]
  (let [target (:target data)
        file   (:file target)]
    [:div.mt2.relative
     [:video.relative.z0
      {:controls true
       :autoplay true
       :style    {:width "100%"}}
      [:source {:src (:url file)}]]
     [:div.white.absolute.left-0.top-0.z1.px2.py1
      [:div.h3.bold.pr1 "Watch:"]
      (:title target)]]))


(defmethod build-hiccup-tag :rich-text/text [{:keys [marks value]}]
  ;; TODO: Strip initial newlines?
  (let [mark-set (into #{} (comp (map :type)
                                 (map keyword)) marks)]
    [:div {:class (cond-> #{}
                  (:bold mark-set) (conj "bold")
                  (:italic mark-set) (conj "italic"))}

     value]))

(defmethod build-hiccup-tag :rich-text/document [{:keys [content]}]
  ;; TODO: Strip initial newlines?
  (build-hiccup-content [:div.template-slot-value] content))

(defmethod build-hiccup-tag :default [node]
  ;; TODO: Productionalize this error message
  (println "Attempting to render unknown node type" node)
  nil)

(defn template-slot-kv->hiccup-content
  "Takes CMS template-slot key-value from contentful and tur"
  ([sku template-slot-kv]
   (let [[template-slot-id template-slot-data] template-slot-kv]
     (some->> (template-slot-data :selectable-values)
              (into []
                    (comp
                     (map (fn [selectable-value ]
                            (merge selectable-value
                                   (:selector selectable-value))))
                     (selector/match-essentials
                      ;; TODO Move the code responsible for merging the selector attributes into the top level
                      ;; of the selectable value to the API / Handler areas)
                      (assoc sku :selector/essentials (into #{} (comp
                                                                 (map :selector)
                                                                 (mapcat keys))
                                                            (:selectable-values template-slot-data))))
                     (map :value)
                     (map (fn [node]
                            (assoc node :template-slot/slug (:slug template-slot-data))))
                     (map build-hiccup-tag)))
              (not-empty)
              (conj [template-slot-id])))))


(defn rename-template-slot-ids [cms-dynamic-content-data]
  ;; NOTE: This should only really be used when we can't or won't
  ;;       dictate the slugs of template slots in contentful. If
  ;;       the slugs in contenful match the slugs here no rename
  ;;       is necessary. This allows us to overwrite the older cellar
  ;;       entries without having to add a mapping here; no deploy needed
  ;;
  ;;       See existing template-slot entries in contentful for examples
  (set/rename-keys cms-dynamic-content-data
                   {}))

(defn cms-and-sku->template-slot-hiccup
  [cms-dynamic-content-data sku]
  #?(:cljs
     (into {}
           (keep (partial template-slot-kv->hiccup-content sku))
           (rename-template-slot-ids cms-dynamic-content-data))))
