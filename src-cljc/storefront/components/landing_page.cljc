(ns storefront.components.landing-page
  (:require [storefront.component :as component :refer [defcomponent]]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.contentful :as contentful]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.components.ui :as ui]))

(defn query [data]
  (let [landing-page-slug (-> data (get-in storefront.keypaths/navigation-args) :landing-page-slug keyword)
        cms-data          (get-in data (conj storefront.keypaths/cms-landing-page landing-page-slug))]
    {:layers
     [(let [cms-hero-data (:hero cms-data)]
        {:layer/type :hero
         :opts       {:href      "/adv/match-stylist"
                      :data-test "home-banner"}
         :dsk-url    (-> cms-hero-data :desktop :file :url)
         :mob-url    (-> cms-hero-data :mobile :file :url)
         :alt        (-> cms-hero-data :alt)
         :file-name  (-> cms-hero-data :desktop :file :file-name)})
      {:layer/type   :shop-text-block
       :header/value (:title cms-data)
       :body/value   (:subtitle cms-data)}
      {:layer/type   :shop-ugc
       :header/value "Shop By Look"
       :images       (map (fn [look]
                            {:image-url              (:photo-url look)
                             :alt                    "Look Photo"
                             :label                  (:title look)
                             :cta/navigation-message [events/navigate-shop-by-look-details
                                                      {:look-id (:content/id look)
                                                       :album-keyword :look}]})
                          (:looks cms-data))
       :cta/id       "landing-page-see-more"
       :cta/value    "see more"
       :cta/target   [events/navigate-shop-by-look {:album-keyword :look}]}
      {:layer/type   :shop-text-block
       :header/value "Benefits of HD Lace"
       :body/value   [(ui/img {:src   "//placekitten.com/301/301"
                               :style {:width "100%"}})
                      [:div.content-2 "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur molestie eleifend lorem quis porttitor. Integer malesuada suscipit sapien non tempus. Nunc rhoncus porttitor augue, consectetur venenatis turpis luctus non."]]
       :cta/button?  true
       :cta/value    "Shop HD Lace Products"
       :cta/id       "landing-page-shop-hd-lace-products"
       :cta/target   [events/navigate-category {:page/slug "mayvenn-install" :catalog/category-id "23"}]}
      {:layer/type   :shop-ugc
       :header/value "Popular HD Products"
       :images       [{:image-url              "//placekitten.com/208/208"
                       :alt                    "cat"
                       :label                  "HD Lace Brazilian Deep Wave Closure"
                       :cta/navigation-message [events/navigate-home]}
                      {:image-url              "//placekitten.com/205/205"
                       :alt                    "cat"
                       :label                  "HD Lace Brazilian Straight Closure"
                       :cta/navigation-message [events/navigate-home]}
                      {:image-url              "//placekitten.com/206/206"
                       :alt                    "cat"
                       :label                  "HD Lace Indian Loose Wave Frontal"
                       :cta/navigation-message [events/navigate-home]}
                      {:image-url              "//placekitten.com/210/210"
                       :alt                    "cat"
                       :label                  "HD Lace Indian Straight Frontal"
                       :cta/navigation-message [events/navigate-home]}]
       :cta/id       "landing-page-view-all-hd-lace-products"
       :cta/value    "View All HD Lace Products"
       :cta/target   [events/navigate-category {:page/slug "mayvenn-install" :catalog/category-id "23"}]}
      {:layer/type     :shop-bulleted-explainer
       :layer/id       "heres-how-it-works"
       :title/value    ["You buy the hair,"
                        "we cover the install."]
       :subtitle/value ["Here's how it works."]
       :bullets        [{:title/value "Pick Your Dream Look"
                         :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                        {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                         :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                        {:title/value ["Book Any Add-On Service"]
                         :body/value  "Want an additional service? On the cart page, you can book a Natural Hair Trim, Weave Take Down, or Deep Conditioning service. Then, continue toward checkout."}
                        {:title/value "Schedule Your Appointment"
                         :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]}
      (merge {:layer/type :faq}
             (faq/hd-lace-query data (:faq cms-data)))]}))

(defn built-component [data opts]
  (component/build layered/component (query data) nil))
