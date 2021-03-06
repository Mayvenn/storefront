(ns adventure.informational.about-our-hair
  (:require [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths))

(defn query
  [data]
  {:layers [{:layer/type :hero
             :file-name  "about-our-hair-hero"
             :ucare?     true
             :mob-uuid   "74034b48-0fab-4e31-92b2-66db9136472a"
             :dsk-uuid   "1162a59d-162f-4c6b-bb8d-60cd7204644c"}
            {:layer/type             :find-out-more
             :header/value           "We believe that quality should be accessible for all."
             :body/value             "Our bundles, closures, and frontals are crafted with the highest industry standards and come in a variety of textures and colors. The best part? All of our products are quality-guaranteed."
             :cta/value              "Get started"
             :cta/navigation-message [events/navigate-adventure-find-your-stylist nil]}
            {:layer/type      :shop-bulleted-explainer
             :header/value    "About Our Hair"
             :subheader/value "An overview"
             :bullets         [{:icon/uuid    "8787e30c-2879-4a43-8d01-9d6790575084"
                                :icon/width   "53"
                                :header/value "Quality-Guaranteed"
                                :body/value   "Wear it, dye it, even cut it. If you're not in love with your hair, we'll exchange it within 30 days."}
                               {:icon/uuid    "e02561dd-c294-43b7-bb33-c40bfabea518"
                                :icon/width   "34"
                                :header/value "Crafted to Last"
                                :body/value   "Our hair is gently steam processed and double machine-wefted to prevent shedding, tangling, and increase longevity."}
                               {:icon/uuid    "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                                :icon/width   "30"
                                :header/value "Care & Support"
                                :body/value   "We have a team of customer service professionals dedicated to answering your questions and giving care advice."}]}
            {:layer/type      :ugc
             :header/value    "#MayvennFreeInstall"
             :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
             :images          (contentful/album-kw->homepage-social-cards (get-in data storefront.keypaths/cms-ugc-collection)
                                                                          :free-install-mayvenn)}
            (merge {:layer/type :faq} (faq/free-install-query data))
            (merge {:layer/type :shop-contact} layered/shop-contact-query)
            {:layer/type             :sticky-footer
             :layer/id               "sticky-footer-about-our-hair"
             :sticky/content         "It’s true, we are paying for your install! "
             :cta/label              "Get started"
             :cta/navigation-message [events/navigate-adventure-find-your-stylist]}]})

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-about-our-hair
  [_ event _ _ app-state]
  (if (not= :shop (sites/determine-site app-state))
    (effects/redirect events/navigate-home)
    #?(:cljs
       (effects/fetch-cms-keypath app-state [:faq :free-mayvenn-services]))))
