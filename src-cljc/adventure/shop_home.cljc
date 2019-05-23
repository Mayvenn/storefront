(ns adventure.shop-home
  (:require [adventure.components.layered :as layered]
            adventure.handlers ;; Needed for its defmethods
            [adventure.keypaths :as keypaths]
            [adventure.faq :as faq]
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events
                       [storefront.browser.scroll :as scroll]])
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.components.video :as video]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [clojure.string :as string]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.routes :as routes]
            [clojure.set :as set]))

(def ^:private default-utm-params
  {:utm_medium "referral"
   :utm_term   "fi_shoptofreeinstall"})

(defn ^:private route-to-or-redirect-to-freeinstall [shop? environment navigation-event navigation-arg]
  (let [navigation-message [navigation-event navigation-arg]]
    (merge (when-not shop?
             {:navigation-message navigation-message})
           {:href (layered/freeinstall-domain environment (apply routes/path-for navigation-message))})))

(defn ^:private cta-route-to-or-redirect-to-freeinstall [shop? environment navigation-event navigation-arg]
  (set/rename-keys (route-to-or-redirect-to-freeinstall shop? environment navigation-event nil)
                   {:href               :cta/href
                    :navigation-message :cta/navigation-message}))

(defn query
  [data]
  (let [shop?                (= "shop" (get-in data storefront.keypaths/store-slug))
        environment          (get-in data storefront.keypaths/environment)
        browse-stylist-hero? (experiments/browse-stylist-hero? data)]
    {:layers
     [(merge {:layer/type      :hero-with-links
              :photo/file-name "free-install-hero"
              :buttons         (into (if browse-stylist-hero?
                                       []
                                       [[{:href         "#learn-more"
                                          :height-class "py2"
                                          :data-test    "learn-more"}
                                         "Learn More"]])
                                     [[(merge (route-to-or-redirect-to-freeinstall
                                               shop? environment
                                               events/navigate-adventure-install-type
                                               {:query-params (merge default-utm-params
                                                                     {:utm_source "toadventurehomepagehero"})})
                                              {:data-test    "adventure-home-choice-get-started"
                                               :height-class "py2"})
                                       "Get Started"]])}
             (if browse-stylist-hero?
               {:photo/mob-uuid "7edde421-146c-407f-be8b-87db0c81ae54"
                :photo/dsk-uuid "41adade2-0987-4f8f-9bed-99d9586fead3"}
               {:photo/alt      "We're changing the game. Introducing Mayvenn Install Hair + Service for the price of one"
                :photo/mob-uuid "8b5bc7af-ca65-4812-88c2-e1601cb17b54"
                :photo/dsk-uuid "6421450f-071d-43ab-b5c9-69de8280d07b"}))
      {:layer/type :free-standard-shipping-bar}
      {:layer/type   :text-block
       :header/value "We're paying for your next hair appointment"
       :body/value   "Purchase 3 or more bundles (closures or frontals included) and we’ll pay for you to get them installed. That’s a shampoo, condition, braid down, sew-in, and style, all on us."
       :anchor/name  "learn-more"}
      {:layer/type      :checklist
       :subheader/value "What's included?"
       :bullets         ["Shampoo and condition"
                         "Braid down"
                         "Sew-in and style"
                         "Paid for by Mayvenn"]}
      {:layer/type :video-overlay
       :video      (get-in data keypaths/adventure-home-video)}
      {:layer/type             :video-block
       :header/value           "#MayvennFreeInstall"
       :body/value             [:span "Learn how you can get your "
                                [:span.nowrap "FREE install"]]
       :cta/value              "Watch Now"
       :cta/navigation-message [(if shop?
                                  events/navigate-home
                                  events/navigate-adventure-home)
                                {:query-params {:video "free-install"}}]}
      (merge
       {:layer/type      :bulleted-explainer
        :header/value    "How it Works"
        :subheader/value "It's simple"
        :bullets         [{:icon/uuid    "3d2b326c-7773-4672-827e-f13dedfae15a"
                           :icon/width   "22"
                           :header/value "1. Choose a Mayvenn Certified Stylist"
                           :body/value   "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."}
                          {:icon/uuid    "08e9d3d8-6f3d-4b3c-bc46-3590175a9a4d"
                           :icon/width   "24"
                           :header/value "2. Buy ANY Three Bundles or More"
                           :body/value   "This includes closures, frontals, and 360 frontals. Risk free - your virgin hair and service are covered by our 30 day guarantee."}
                          {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
                           :icon/width   "27"
                           :header/value "3. Schedule Your Appointment"
                           :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}]
        #_#_
        :cta/value "Learn more"}
       #_
       (cta-route-to-or-redirect-to-freeinstall
        shop? environment events/navigate-info-about-our-hair nil))
      {:layer/type   :text-block
       :header/value "Who's doing my hair?"
       :body/value   "Our Certified Stylists are the best in your area. They're chosen because of their top-rated reviews, professionalism, and amazing work."}
      {:layer/type      :image-block
       :photo/mob-uuid  "a6a607e6-aeb4-4b61-8bc7-60fd17d15abe"
       :photo/dsk-uuid  "f2d82c41-2051-47d8-86c5-1c82568e324d"
       :photo/file-name "who-shop-hair"}
      {:layer/type   :text-block
       :header/value "Quality-Guaranteed Virgin Hair"
       :body/value   "Our bundles, closures, and frontals are crafted with the highest industry standards and come in a variety of textures and colors."}
      {:layer/type      :image-block
       :photo/mob-uuid  "e994076c-b21f-4925-b72b-f804b7408599"
       :photo/dsk-uuid  "ddce59fd-2607-4415-a3e1-e1f12c459dc6"
       :photo/file-name "quality-guaranteed"}
      {:layer/type      :ugc
       :header/value    "#MayvennFreeInstall"
       :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
       :images          (pixlee/images-in-album
                         (get-in data storefront.keypaths/ugc)
                         :free-install-mayvenn)}
      (merge {:layer/type :faq} (faq/free-install-query data))
      (when shop? {:layer/type :escape-hatch})
      {:layer/type      :bulleted-explainer
       :header/value    "Mayvenn is More than a Hair Company"
       :subheader/value "It's a movement"
       :bullets         [{:icon/uuid    "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                          :icon/width   "29"
                          :header/value "Top-Notch Customer Service"
                          :body/value   "Our team is made up of hair experts ready to help you by phone, text, and email."}
                         {:icon/uuid    "8787e30c-2879-4a43-8d01-9d6790575084"
                          :icon/width   "52"
                          :header/value "30 Day Guarantee"
                          :body/value   "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
                         {:icon/uuid    "e02561dd-c294-43b7-bb33-c40bfabea518"
                          :icon/width   "35"
                          :header/value "100% Virgin Hair"
                          :body/value   "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
                         {:icon/uuid    "5a04ea88-b0f8-416b-a380-1da0baa8a114"
                          :icon/width   "35"
                          :header/value "Certified Stylists"
                          :body/value   "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]}
      {:layer/type             :homepage-were-changing-the-game
       :cta/navigation-message [(if shop?
                                  events/navigate-home
                                  events/navigate-adventure-home)
                                {:query-params {:video "we-are-mayvenn"}}]}
      {:layer/type :contact}
      (merge
       {:layer/type :sticky-footer}
       (cta-route-to-or-redirect-to-freeinstall
        shop? environment
        events/navigate-adventure-install-type
        {:query-params (merge default-utm-params
                              {:utm_source "toadventurehomepagestickybar"})}))]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/control-open-shop-escape-hatch
  [_ _ _ _ _]
  (messages/handle-message events/control-menu-expand-hamburger
                           {:keypath storefront.keypaths/menu-expanded})
  #?(:cljs (scroll/snap-to-top)))
