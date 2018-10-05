(ns install.certified-stylists
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.events.EventType :as EventType]])
            [spice.core :as spice]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.numbers :as numbers]
            [storefront.platform.messages :as messages]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [clojure.string :as string]
            [spice.maps :as maps]
            [cemerick.url :as url]))

(defn star [type]
  [:span.mrp1
   (ui/ucare-img
    {:width "13"}
    (case type
      :whole         "5a9df759-cf40-4599-8ce6-c61502635213"
      :three-quarter "a34fada5-aad8-44a7-8113-ddba7910947d"
      :half          "d3ff89f5-533c-418f-80ef-27aa68e40eb1"
      :empty         "92d024c6-1e82-4561-925a-00d45862e358"
      nil))])

(defn star-rating
  [rating]
  (let [remainder-rating (mod 5 rating)
        whole-stars      (repeat (int rating) (star :whole))
        partial-star     (cond
                           (== 0 remainder-rating)
                           nil

                           (== 0.5 remainder-rating)
                           (star :half)

                           (> 0.5 remainder-rating)
                           (star :three-quarter)

                           :else
                           nil)
        empty-stars (repeat (- 5
                               (count whole-stars)
                               (if partial-star 1 0))
                            (star :empty))]
    [:div.flex.items-center
     whole-stars
     partial-star
     empty-stars
     [:span.mlp2.h6 rating]]))

(defn stylist-attribute
  [icon-width ucare-id content]
  [:div.flex.items-center
   [:div.mr1 {:style {:width "10px"}}
    (ui/ucare-img {:width icon-width} ucare-id)]
   content])

(def stylists
  [{:first-name          "Aundria"
    :last-name           "Carter"
    :gallery-images      ["91566919-324f-4714-9043-f4c24ae2fe98"
                          "caae31b7-36f3-439c-9c14-af0dd83f31e7"
                          "84ecceb8-443a-40fa-936a-6c639bf8280a"
                          "1cc6eff6-308b-4f66-8fa8-98287f56c4e3"
                          "7421e36b-1d41-4747-a6da-b5374a5bfbba"
                          "95c3038d-025d-4185-a607-96356c6867a8"
                          "46510df0-e1b2-45bc-8744-dec585ef165f"
                          "7c7a433f-40e8-4a0d-af74-1b8ff2e69f7e"
                          "f5dd7a95-53c9-4c2f-a380-3698852615e7"
                          "343835b4-1b1d-44a3-9162-a690531f3edd"
                          "08199689-1514-4ae3-bebc-00242dd96f51"]
    :portrait-image-id   "63acc2ac-43cc-48cb-9db7-0361f01aaa25"
    :bio                 ["Hey, I’m Aundria aka Kesh Styles."
                          " I’ve known that doing hair was my calling ever since I was a little girl."
                          " I tend to specialize in sew-ins and quick weaves,"
                          " and seeing clients happy is my ultimate goal."
                          " Reach out to me so we can started on your new style!"]
    :location            "By Faith Beauty Haven"
    :phone               "(910) 920-5718"
    :rating              4.8
    :years-of-experience 10}

   {:first-name          "Angela"
    :last-name           "White"
    :gallery-images      ["2e6076b8-dcbe-44ba-98b5-3402592d5858"
                          "7c59eea0-952b-4775-a50c-d8b638d6cb48"
                          "b356c268-7624-4ef9-89e3-3f5e065f6511"
                          "2c7cd91a-50ff-4a49-9483-1962714e6493"
                          "ca6f688f-1822-432c-ae28-73160e965a00"
                          "69c46710-3958-4049-bb21-e435f8b33ad9"
                          "7df1ce4b-9e85-4674-968c-dc61bc00c122"]
    :portrait-image-id   "a7903783-7c7a-4459-85a7-fc9db361696e"
    :bio                 ["My name is Angela and I’ve been a hairstylist for 19 years."
                          " My specialties include quick weaves, sew-ins, and cutting and styling."
                          " As a seasoned stylist, making people look good and"
                          " feel glorious is my passion."]
    :location            "Parise Exquisite"
    :phone               "(910) 964-4976"
    :rating              4.75
    :years-of-experience 19}

   {:first-name          "Tamara"
    :last-name           "Johnson"
    :gallery-images      ["def0352c-0d85-49cc-904c-f6ced1482cc5"
                          "c6710751-ea7d-4e75-b301-096292550326"
                          "1be01c11-10ee-4b78-8252-0c345c5cf45e"
                          "0820e925-fcd8-440f-bb32-30490e8dd676"
                          "4e5a17f2-f721-49dc-81d7-d495cae08fef"
                          "c2e137cc-a147-4f27-907f-08d13e2c929f"
                          "b9cabcaf-9fcb-4d22-970b-ba3192a17abc"
                          "21fae71d-e749-461f-8f1e-15c242e73f3f"
                          "0a3cd9a0-308b-4de1-a563-553aa88fa66d"]
    :portrait-image-id   "be913d9e-e69d-45e9-8a92-23b7dfca01fe"
    :bio                 ["Hi, my name is Tamara and I’ve been a professional stylist for 9 years."
                          " Hair has been a huge part of my life since I was a little girl."
                          " I’m ready to help you choose the perfect bundles and do your sew-in."
                          " You’re in good hands!"]
    :location            "Sara’s Hair and Nail Salon"
    :phone               "(910) 476-0909"
    :rating              4.9
    :years-of-experience 9}

   {:first-name          "Valerie"
    :last-name           "Selby"
    :gallery-images      ["055147ca-5cd9-41c6-8038-18125454c478"
                          "981e5b31-3dc8-4f3e-814a-8899ff78f395"
                          "66e9dd27-5367-4d6b-90f2-a08670f9164b"
                          "cb4b1290-32a0-4a16-b3df-1c214a886201"
                          "94d355f2-fcbb-4d81-824c-9284ee5a993d"
                          "4c16e602-3c11-441a-b035-78a986cb1b9c"
                          "b9048896-2123-43c4-b503-786425ceee9c"
                          "8adaf2dc-3d3e-4045-8549-85bf2b28f71e"
                          "76a6083d-1f14-4660-b70f-17134aae07cc"]
    :portrait-image-id   "f1ba9936-d310-42fb-a0fa-fa54b49e7055"
    :bio                 ["My name is Valerie and I’ve been doing hair since I was 11 years old."
                          " I’ve been licensed for 15 years and hair is my true passion."
                          " Healthy hair is my first priority for any client."
                          " I specialize in all hair types including natural and relaxed hair,"
                          " weaves, and crochet styles."]
    :location            "Illstylz Salon"
    :phone               "(910) 494-6261"
    :rating              4.85
    :years-of-experience 15}

   {:first-name          "Simone"
    :last-name           "Ambrose"
    :gallery-images      ["5914100a-78b8-416b-9803-57061cb6d63f"
                          "11306923-06d8-4d60-9135-e9334026477b"
                          "e41b9297-f437-4bb9-9256-1e4052e6265a"
                          "5d25ba59-fe72-483e-873e-7c6c6d1aaef8"
                          "d8031cb8-13f4-4bad-85b9-f6b17873f9e6"
                          "f42751ee-9790-49f0-aed9-fdb92f5792b5"]
    :portrait-image-id   "de190b0a-4e04-4604-8be6-2438c93c6059"
    :bio                 ["My name is Simone Ambrose, I’m the owner of The Beauty Room."
                          " With over 10 years of experience in the beauty industry,"
                          " I’ve traveled and transformed styles from Paris to Germany."
                          " After working on fashion shows in New York, LA, DC, and New Orleans,"
                          " I’m serving my hometown of Fayetteville."
                          " I specialize in health for all hair types, namely hair extensions,"
                          " eyelash extensions, braids and natural hair care."]
    :location            "The Beauty Room"
    :phone               "(910) 366-8039"
    :rating              5
    :years-of-experience 12}])

(defn ^:private gallery-slide [index ucare-id]
  [:div {:key (str "gallery-slide" index)}
   (ui/aspect-ratio 1 1
                    (ui/ucare-img {:class "col-12"} ucare-id))])

(defn stylist-card
  [{:keys [selected-stylist-index
           selected-image-index
           current-stylist-index
           gallery-open?]}
   {:keys [gallery-images
           portrait-image-id
           location
           phone
           licensed?
           years-of-experience
           rating
           first-name
           last-name
           bio]}]
  [:div.bg-white.p2.pb2.h6.my2.mx2-on-tb-dt.col-12.col-5-on-tb-dt {:key first-name}
   [:div.flex
    [:div.mr2.mt1 (ui/circle-ucare-img {:width "104"} portrait-image-id)]
    [:div.flex-grow-1
     [:div.h4 (clojure.string/join  " " [first-name last-name])]
     [:div (star-rating rating)]
     (stylist-attribute "8" "d1e19d12-edcb-4068-9fa4-f75c94d3b7e6" [:span.bold location])
     (stylist-attribute "10" "2fa4458a-ce39-4a73-920e-c0e58ca7ffcd" phone)
     (stylist-attribute "10" "da021ef5-4190-4c19-b729-33fcf5b68d01" "Licensed Salon Stylist")
     (stylist-attribute "10" "3987ebfd-8f8b-4883-ac1c-f9929e6ea6a3" (str years-of-experience " yrs Experience"))]]
   [:div.line-height-2.medium.dark-gray.mt1 {:style {:min-height "75px"}} bio]
   [:div.my2.m1-on-tb-dt
    (component/build carousel/component
                     {:slides   (map-indexed (fn [i x]
                                               [:div
                                                {:on-click #(messages/handle-message
                                                             events/control-stylist-gallery-open
                                                             {:stylist-gallery-index current-stylist-index
                                                              :image-index           i})
                                                 :key      (str first-name "-gallery-" i)}
                                                (ui/aspect-ratio
                                                 1 1
                                                 (ui/ucare-img {:width "102"} x))]) gallery-images)
                      :settings {:swipe        true
                                 :initialSlide 0
                                 :arrows       true
                                 :dots         false
                                 :slidesToShow 3
                                 :infinite     true}}
                     {})]
   (ui/teal-button {:href (str "sms:" (numbers/digits-only phone))}
                   [:div.flex.items-center.justify-center.mynp3.inherit-color
                    [:span.mr1.pt1 (ui/ucare-img {:width "32"} "c220762a-87da-49ac-baa9-0c2479addab6")]
                    (str "Text " first-name)])
   (when gallery-open?
     (let [close-attrs (utils/fake-href events/control-stylist-gallery-close)]
       (ui/modal
        {:close-attrs close-attrs
         :col-class   "col-12"}
        [:div.relative.mx-auto
         {:style {:max-width "750px"}}
         (component/build carousel/component
                          {:slides   (map-indexed gallery-slide gallery-images)
                           :settings {:initialSlide (or selected-image-index 0)
                                      :slidesToShow 1}}
                          {})
         [:div.absolute
          {:style {:top "1.5rem" :right "1.5rem"}}
          (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                           :close-attrs close-attrs})]])))])

(defn component
  [{:keys [stylist-gallery-index gallery-image-index]} owner opts]
  (component/create
   [:div.px3.p1.bg-light-silver.flex-wrap.flex.justify-center
    [:div.col-12.col-8-on-dt.py2.flex-wrap.flex.justify-center
     (map-indexed
      (fn [index stylist]
        (stylist-card
         {:selected-stylist-index stylist-gallery-index
          :selected-image-index   gallery-image-index
          :current-stylist-index  index
          :gallery-open?          (= stylist-gallery-index index)}
         stylist))
      stylists)]]))

(defmethod transitions/transition-state events/control-stylist-gallery-open [_ _event {:keys [stylist-gallery-index image-index]} app-state]
  (-> app-state
      (assoc-in keypaths/carousel-stylist-gallery-index stylist-gallery-index)
      (assoc-in keypaths/carousel-stylist-gallery-image-index image-index)))

(defmethod transitions/transition-state events/control-stylist-gallery-close [_ _event _args app-state]
  (-> app-state
      (update-in keypaths/carousel-stylist-gallery dissoc :index)
      (update-in keypaths/carousel-stylist-gallery dissoc :image-index)))
