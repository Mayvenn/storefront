(ns storefront.components.homepage-hero
  (:require [storefront.routes :as routes]
            [storefront.events :as events]))

(defn query
  [cms-hero-data]
  (let [path                    (or (:path cms-hero-data) "/shop/look")
        [event :as routed-path] (routes/navigation-message-for path)
        link-options            (assoc (if-not (= events/navigate-not-found event)
                                         {:navigation-message routed-path}
                                         {:href path}) :data-test "home-banner")]
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
