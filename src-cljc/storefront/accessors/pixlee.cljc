(ns storefront.accessors.pixlee
  (:require [cemerick.url :as url]
            [clojure.set :as set]
            [clojure.string :as string]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]))

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

(defn parse-ugc-image [album-slug {:keys [album_id album_photo_id user_name content_type source products title source_url] :as item}]
  (let [[nav-event nav-args :as nav-message] (product-link (first products))]
    {:id             album_photo_id
     :content-type   content_type
     :source-url     source_url
     :user-handle    (normalize-user-name user_name)
     :imgs           (extract-images item)
     :social-service source
     :shared-cart-id (:shared-cart-id nav-args)
     :links          (merge {:view-other nav-message}
                            (when (= nav-event events/navigate-shared-cart)
                              {:view-look [events/navigate-shop-by-look-details {:album-slug (or (#{:deals} album-slug)
                                                                                                 :look)
                                                                                 :look-id album_photo_id}]}))
     :title          title}))

(defn parse-ugc-album [album-slug album]
  (map (partial parse-ugc-image album-slug) album))

(defn images-by-id [images]
  (reduce (fn [result img]
            (assoc result (:id img) img))
          {}
          images))

(defn images-in-album [ugc album]
  (let [image-ids (get-in ugc [:albums album])]
    (into []
          (comp
           (map (get ugc :images))
           (remove (comp #{"video"} :content-type)))
          image-ids)))

(defn selected-look [data]
  (get-in data (conj keypaths/ugc-images (get-in data keypaths/selected-look-id))))
