(ns adventure.shop-home
  (:require [adventure.components.layered :as layered]
            adventure.handlers ;; Needed for its defmethods
            [storefront.accessors.contentful :as contentful]
            [storefront.keypaths :as storefront.keypaths]
            [adventure.keypaths :as keypaths]
            [adventure.faq :as faq]
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events
                       [storefront.browser.scroll :as scroll]])
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]))

(defn query
  [data]
  (let [shop?              (= "shop" (get-in data storefront.keypaths/store-slug))
        adventure-on-shop? (experiments/adventure-on-shop? data)
        environment        (get-in data storefront.keypaths/environment)
        cms-ugc-collection (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event  (get-in data storefront.keypaths/navigation-event)
        nav-args           {:shop?              shop?
                            :adventure-on-shop? adventure-on-shop?
                            :environment        environment}]
    (letfn [(-route-or-redirect [nav-args event utm-source]
              (layered/cta-route-to-or-redirect-to-freeinstall
               (merge nav-args {:shop?              shop?
                                :environment        environment
                                :adventure-on-shop? adventure-on-shop?
                                :navigation-event   event})))]
      {:layers
       [(merge {:layer/type      :hero
                :photo/file-name "free-install-hero"
                :buttons         [[(merge (layered/route-to-or-redirect-to-freeinstall
                                           (merge nav-args
                                                  {:shop?              shop?
                                                   :environment        environment
                                                   :adventure-on-shop? adventure-on-shop?
                                                   :navigation-event   events/navigate-adventure-match-stylist}))
                                          {:data-test    "adventure-home-choice-get-started"
                                           :height-class "py2"})
                                   "Browse Stylists"]]}
               {:photo/mob-uuid "7edde421-146c-407f-be8b-87db0c81ae54"
                :photo/dsk-uuid "41adade2-0987-4f8f-9bed-99d9586fead3"})
        {:layer/type :free-standard-shipping-bar}
        {:layer/type   :text-block
         :header/value "We're paying for your next hair appointment"
         :body/value   "Purchase 3 or more bundles, closures or frontals and we’ll pay for you to get them installed. That’s a shampoo, condition, braid down, sew-in, and style, all on us."
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
         :body/value             [:span "Learn how you can get your free "
                                  [:span.nowrap "Mayvenn Install"]]
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
                             :header/value "2. Buy Any 3 Items or More"
                             :body/value   "Purchase 3 or more bundles, closures or frontals. Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."}
                            {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
                             :icon/width   "27"
                             :header/value "3. Schedule Your Appointment"
                             :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}]
          :cta/value       "Learn more"}
         (-route-or-redirect nav-args events/navigate-info-how-it-works "toadventurelearnmore"))
        (merge
         {:layer/type   :text-block
          :header/value "Who's doing my hair?"
          :body/value   "Our Certified Stylists are the best in your area. They're chosen because of their top-rated reviews, professionalism, and amazing work."
          :cta/value    "Learn more"}
         (-route-or-redirect nav-args events/navigate-info-certified-stylists "toadventurelearnmore"))
        {:layer/type      :image-block
         :photo/mob-uuid  "a6a607e6-aeb4-4b61-8bc7-60fd17d15abe"
         :photo/dsk-uuid  "f2d82c41-2051-47d8-86c5-1c82568e324d"
         :photo/file-name "who-shop-hair"}
        (merge
         {:layer/type   :text-block
          :header/value "Quality-Guaranteed Virgin Hair"
          :body/value   "Our bundles, closures, and frontals are crafted with the highest industry standards and come in a variety of textures and colors."
          :cta/value    "Learn more"}
         (-route-or-redirect nav-args events/navigate-info-about-our-hair "toadventurelearnmore"))
        {:layer/type      :image-block
         :photo/mob-uuid  "e994076c-b21f-4925-b72b-f804b7408599"
         :photo/dsk-uuid  "ddce59fd-2607-4415-a3e1-e1f12c459dc6"
         :photo/file-name "quality-guaranteed"}
        {:layer/type      :ugc
         :header/value    "#MayvennFreeInstall"
         :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
         :images          (mapv (partial contentful/look->homepage-social-card
                                         current-nav-event
                                         :free-install-mayvenn)
                                (->> cms-ugc-collection :free-install-mayvenn :looks))}
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
         (-route-or-redirect nav-args events/navigate-adventure-match-stylist nil))]})))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/control-open-shop-escape-hatch
  [_ _ _ _ _]
  (messages/handle-message events/control-menu-expand-hamburger
                           {:keypath storefront.keypaths/menu-expanded})
  #?(:cljs (scroll/snap-to-top)))
