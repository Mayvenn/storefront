(ns adventure.informational.how-it-works
  (:require storefront.keypaths
            [storefront.accessors.pixlee :as pixlee]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [adventure.faq :as faq]
            [storefront.components.ugc :as ugc]
            [storefront.accessors.experiments :as experiments]
            [adventure.components.layered :as layered]
            #?@(:cljs [[storefront.hooks.pixlee :as pixlee.hook]])))

(defn query
  [data]
  (let [cms-ugc-collection    (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event     (get-in data storefront.keypaths/navigation-event)
        pixlee-to-contentful? (experiments/pixlee-to-contentful? data)]
    {:layers [{:layer/type      :hero
               :photo/file-name "how-it-works-hero"
               :photo/alt       "Mayvenn's Install includes: Shampoo & Condition, Braid down, Sew-In, Style"
               :photo/mob-uuid  "99a8ccca-b4fe-42d2-8ac1-7ebf8e4f6559"
               :photo/dsk-uuid  "cf2ba376-a913-49de-a2bd-e90c50c295ff"}
              {:layer/type             :find-out-more
               :header/value           "We’re offering hair + service for the price of one"
               :body/value             "We know that quality bundles can be expensive. That’s why when you buy our hair, we’re paying for your install appointment."
               :cta/value              "Get started"
               :cta/navigation-message [events/navigate-adventure-install-type nil]}
              {:layer/type      :bulleted-explainer
               :header/value    "How it Works"
               :subheader/value "In 3 easy steps"
               :bullets         [{:icon/uuid              "3d2b326c-7773-4672-827e-f13dedfae15a"
                                  :icon/width             "22"
                                  :header/value           "1. Choose a Mayvenn Certified Stylist"
                                  :body/value             "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."
                                  :cta/value              "Learn more"
                                  :cta/navigation-message [events/navigate-info-certified-stylists nil]}
                                 {:icon/uuid              "08e9d3d8-6f3d-4b3c-bc46-3590175a9a4d"
                                  :icon/width             "24"
                                  :header/value           "2. Buy ANY Three Bundles or More"
                                  :body/value             "This includes closures, frontals, and 360 frontals. Risk free - your virgin hair and service are covered by our 30 day guarantee."
                                  :cta/value              "Learn more"
                                  :cta/navigation-message [events/navigate-info-about-our-hair nil]}
                                 {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
                                  :icon/width   "27"
                                  :header/value "3. Schedule Your Appointment"
                                  :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}]}
              {:layer/type      :ugc
               :header/value    "#MayvennFreeInstall"
               :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
               :images          (if pixlee-to-contentful?
                                  (mapv (partial ugc/contentful-look->homepage-social-card
                                                 current-nav-event
                                                 :free-install-mayvenn)
                                        (->> cms-ugc-collection :free-install-mayvenn :looks))
                                  (mapv ugc/pixlee-look->homepage-social-card
                                        (pixlee/images-in-album
                                         (get-in data storefront.keypaths/ugc) :free-install-mayvenn)))}
              (merge {:layer/type :faq}
                     (faq/free-install-query data))
              {:layer/type :contact}
              {:layer/type             :sticky-footer
               :cta/navigation-message [events/navigate-adventure-install-type nil]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-how-it-works
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
