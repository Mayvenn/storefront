(ns storefront.accessors.pixlee
  (:require [cemerick.url :as url]
            [clojure.set :as set]
            [storefront.accessors.experiments :as experiments]
            [clojure.string :as string]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.config :as config]))

(defn normalize-user-name [user-name]
  (if (= (first user-name) \@)
    (apply str (rest user-name))
    user-name))

(defn- extract-img-urls [coll original large medium small]
  (-> coll
      (select-keys [original large medium small])
      (set/rename-keys {original :original
                        large    :large
                        medium   :medium
                        small    :small})
      (->>
       (remove (comp string/blank? val))
       (into {}))))

(defn- extract-images [{:keys [title pixlee_cdn_photos] :as item}]
  (reduce-kv (fn [result name url] (assoc result name {:src url :alt title}))
             {}
             (merge
              (extract-img-urls item :source_url :big_url :medium_url :thumbnail_url)
              (extract-img-urls pixlee_cdn_photos :original_url :large_url :medium_url :small_url))))

(defn- product-link [product]
  (-> product
      :link
      url/url-decode
      url/url
      :path
      routes/navigation-message-for))

(defn ^:private notes->look-attributes
  "Expects a url encoded string that is delimited by asterisks with texture,
  color, and length"
  [notes]
  (let [[texture color lengths price :as parsed-notes] (some-> notes url/url-decode (string/split #"\*"))]
    (case (count parsed-notes)
      3 {:texture texture
         :color   color
         :lengths lengths}
      4 {:texture texture
         :color   color
         :lengths lengths
         :price   price}
      nil)))

(defn parse-ugc-image [album-keyword {:keys [notes album_id album_photo_id user_name content_type source products title source_url] :as item}]
  (let [[nav-event nav-args :as nav-message] (product-link (first products))]
    {:id              album_photo_id
     :content-type    content_type
     :source-url      source_url
     :user-handle     (normalize-user-name user_name)
     :imgs            (extract-images item)
     :social-service  source
     :shared-cart-id  (:shared-cart-id nav-args)
     :look-attributes (notes->look-attributes notes)
     :links           (merge
                       {:view-other nav-message}
                       (cond
                         (and (= nav-event events/navigate-shared-cart)
                              (#{:adventure :adventure-bundle-set} album-keyword))
                         {:view-look (spice.core/spy [events/navigate-adventure-look-detail {:album-keyword album-keyword
                                                                                             :look-id       album_photo_id}])}

                         (= nav-event events/navigate-shared-cart)
                         {:view-look [events/navigate-shop-by-look-details {:album-keyword (or (#{:deals} album-keyword) :look)
                                                                            :look-id       album_photo_id}]}

                         :else nil))
     :title title}))

(defn parse-ugc-album [album-keyword album]
  (map (partial parse-ugc-image album-keyword) album))

(defn images-by-id [images]
  (reduce (fn [result img]
            (assoc result (:id img) img))
          {}
          images))

(defn images-in-album [ugc album-keyword]
  (let [image-ids (get-in ugc [:albums album-keyword])]
    (into []
          (comp
           (map (get ugc :images))
           (remove (comp #{"video"} :content-type)))
          image-ids)))

(defn selected-look [data]
  (get-in data
          (conj keypaths/ugc-images
                (get-in data keypaths/selected-look-id))))

(defn determine-look-album
  [data target-album-keyword]
  (let [the-ville?       (experiments/the-ville? data)
        v2-experience?   (experiments/v2-experience? data)

        actual-album (cond

                       (not= target-album-keyword :look)
                       target-album-keyword

                       v2-experience?
                       :aladdin-free-install

                       the-ville?
                       :free-install

                       :elsewise target-album-keyword)]
    (if (-> config/pixlee :albums (contains? actual-album))
      actual-album
      :pixlee/unknown-album)))
