(ns adventure.informational.certified-stylists
  (:require storefront.keypaths
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.components.ugc :as ugc]
            [storefront.accessors.experiments :as experiments]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            #?@(:cljs [[storefront.hooks.pixlee :as pixlee.hook]])))

(defn query
  [data]
  (let [cms-ugc-collection    (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event     (get-in data storefront.keypaths/navigation-event)
        pixlee-to-contentful? (experiments/pixlee-to-contentful? data)]
    {:layers [{:layer/type      :hero
               :photo/file-name "certified-stylists-hero"
               :photo/mob-uuid  "0d06e8a8-123b-4265-b988-e7d55bddd7f0"
               :photo/dsk-uuid  "3cd59dc1-8f94-4ee5-a62e-d91b5aa6c97b"}
              {:layer/type             :find-out-more
               :header/value           "Our Certified Stylists are the best of the best."
               :body/value             (str "Our Certified Stylists are the best in your area. "
                                            "They’re chosen because of their top-rated reviews, professionalism, and amazing work.")
               :cta/value              "Get started"
               :cta/navigation-message [events/navigate-adventure-install-type nil]}

              {:layer/type      :bulleted-explainer
               :header/value    "About Our Certified Stylists"
               :subheader/value "An overview"

               :bullets [{:icon/uuid    "6f63157c-dc3a-4bbb-abcf-e03b08d6e102"
                          :icon/width   "27"
                          :header/value "Licensed Stylists"
                          :body/value   "Our certified stylists are licensed, regulated service practitioners in each state."}
                         {:icon/uuid    "deeaa11d-c48a-4657-8d01-f477d2ea18a5"
                          :icon/width   "24"
                          :header/value "Salon Professionals"
                          :body/value   (str "We believe that quality beauty should be accessible for all. "
                                             "Our stylists work in professional, clean salon spaces.")}
                         {:icon/uuid    "b862f042-ad8d-4230-8ce4-20059dd540d7"
                          :icon/width   "34"
                          :header/value "Top Ratings & Reviews"
                          :body/value   (str "From Yelp to client surveys, we ensure that our stylists adhere"
                                             " to a top-notch code of ethics and professionalism.")}]}
              {:layer/type      :ugc
               :header/value    "#MayvennFreeInstall"
               :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
               :images          (if pixlee-to-contentful?
                                  (mapv (partial contentful/look->homepage-social-card
                                                 current-nav-event
                                                 :free-install-mayvenn)
                                        (->> cms-ugc-collection :free-install-mayvenn :looks))
                                  (mapv ugc/pixlee-look->homepage-social-card
                                        (pixlee/images-in-album
                                         (get-in data storefront.keypaths/ugc) :free-install-mayvenn)))}
              (merge {:layer/type :faq} (faq/free-install-query data))
              {:layer/type :contact}
              {:layer/type             :sticky-footer
               :cta/navigation-message [events/navigate-adventure-install-type nil]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-certified-stylists
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
