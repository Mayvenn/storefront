;; TODO(corey) This should be moved to homepage ns
;; It goes with homepage js module
(ns storefront.components.homepage-hero
  (:require [storefront.uri :as uri]
            [storefront.routes :as routes]
            [storefront.events :as events]))

(defn info-path? [path]
  (when path (re-find #"/info/..*" path)))

(defn blog-path? [path]
  (when path (re-find #"/blog/*" path)))

(defn query
  [cms-hero-data]
  (let [external-path           (:external-path cms-hero-data)
        path'                   (cms-hero-data :path)
        path                    (when (seq path') (uri/path->path-base path'))
        query-params            (when (seq path') (uri/path->query-params path'))
        [event :as routed-path] (cond
                                  (not (seq path))
                                  nil

                                  external-path
                                  [events/external-redirect-url {:url external-path}]

                                  (info-path? path)
                                  [events/external-redirect-info-page {:info-path path}]

                                  (blog-path? path)
                                  [events/external-redirect-blog-page {:blog-path path}]

                                  :else
                                  (routes/navigation-message-for path query-params))
        link-options (assoc (if (= events/navigate-not-found event)
                              {}
                              {:navigation-message routed-path})
                            :data-test "home-banner")]
    (cond->
        {:opts      link-options
         :dsk-url   (-> cms-hero-data :desktop :file :url)
         :mob-url   (-> cms-hero-data :mobile :file :url)
         :alt       (-> cms-hero-data :alt)
         :file-name (-> cms-hero-data :desktop :file :file-name)}

      (not cms-hero-data)
      (merge
       {:alt       "Hair For Your Every Side - Shop Now"
        :file-name "holiday-01-site-classic-homepage-hero-dsk-03.jpg"
        :dsk-uuid  "9435bf95-0710-4de9-b928-fd8d86871a4c"
        :mob-uuid  "b6536faf-ad41-4deb-838e-24762b29303a"
        :ucare?    true}))))
