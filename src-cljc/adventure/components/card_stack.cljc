(ns adventure.components.card-stack
  (:require [adventure.components.header :as header]
            [adventure.components.profile-card-with-gallery :as profile-card-with-gallery]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            #?@(:cljs [[storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.api :as api]])))

(defn ^:private gallery-slide [index ucare-img-url]
  [:div {:key (str "gallery-slide" index)}
   (ui/aspect-ratio 1 1
                    (ui/ucare-img {:class "col-12"} ucare-img-url))])

(defn gallery-modal-component [{:keys [ucare-img-urls initially-selected-image-index close-button] :as gallery-modal} _ _]
  (component/create
   [:div
    (when (seq ucare-img-urls)
      (let [close-attrs (utils/fake-href (:target-message close-button))]
        (ui/modal
         {:close-attrs close-attrs
          :col-class   "col-12"}
         [:div.relative.mx-auto
          {:style {:max-width "750px"}}
          (component/build carousel/component
                           {:slides   (map-indexed gallery-slide ucare-img-urls)
                            :settings {:initialSlide (or initially-selected-image-index 0)
                                       :slidesToShow 1}}
                           {})
          [:div.absolute
           {:style {:top "1.5rem" :right "1.5rem"}}
           (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                            :close-attrs close-attrs})]])))]))

(defn recommend-stylist-component
  [_ _ _]
  (component/create
   [:div.p8.bg-lavender
    {:style {:background-position "right center"
             :background-repeat   "no-repeat"
             :background-size     "contain"
             :background-image    (str "url('" (str "//ucarecdn.com/" "6a221a42-9a1f-4443-8ecc-595af233ab42" "/-/format/auto/") "')")}}
    [:div.center.col-12
     [:div.h1.white "Wish you could use your own stylist?"]
     [:div.p3.white "Well, your wish is my command ;)"]]
    [:div.col-9.mx-auto
     (ui/teal-button
      (merge {:data-test "recommend-stylist"}
             (utils/fake-href events/external-redirect-typeform-recommend-stylist))
      [:div.flex.items-center.justify-center.inherit-color
       "Submit Your Stylist"])]]))

(defmethod effects/perform-effects events/external-redirect-typeform-recommend-stylist
  [_ _ _ _ _ _]
  #?(:cljs
     (set! (.-location js/window)
           "https://mayvenn.typeform.com/to/J2Y1cC")))

(defn component
  [{:keys [header-data gallery-modal-data cards-data title] :as data} _ _]
  (component/create
   (when (seq cards-data)
     [:div.center.flex-auto.bg-light-lavender
      (component/build gallery-modal-component gallery-modal-data nil)
      [:div.white
       (when header-data
         (header/built-component header-data nil))]
      [:div
       [:div.flex.items-center.bold.bg-light-lavender
        {:style {:height "75px"}}]
       [:div.bg-white
        [:div.flex.flex-auto.justify-center.pt6
         [:div.h3.bold.purple title]]
        [:div.bg-white.flex-wrap.flex.justify-center
         [:div.col-12.py2.flex-wrap.flex.justify-center
          (for [{:keys [key] card-type :card/type :as cd} cards-data]
            (do
              (case card-type
                :stylist-profile   (component/build profile-card-with-gallery/component cd {:key key})
                :recommend-stylist (component/build recommend-stylist-component cd {:key key})
                [:div "no matching clause"])))]]
        (let [{:escape-hatch/keys [navigation-event copy data-test]} data]
          [:div.h6.dark-gray.mt3.pb4
           [:div.col-7-on-tb-dt.col-9.mx-auto.mb1
            "Not ready to pick a stylist? Let a Mayvenn expert find one for you after you buy hair."]
           [:a.teal.medium (merge {:data-test data-test} (utils/route-to navigation-event)) copy]])]]])))
