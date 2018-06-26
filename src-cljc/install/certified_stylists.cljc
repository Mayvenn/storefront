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
            [storefront.platform.messages :as messages]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

(def ^:private stylist-information
  [{:stylist-name     "Aundria Carter"
    :stylist-headshot "63acc2ac-43cc-48cb-9db7-0361f01aaa25"
    :salon-name       "Faith Beauty Haven"
    :salon-address    "5322 Yadkin Rd. Fayetteville, NC"
    :stylist-bio      "Aundria Carter better known as Keshstyles, has logged over 10 years of experience as a stylist. She ensures a memorable and enjoyable experience while getting the style of your dreams. A sew-in specialist she focuses on accentuating the beauty of all of her clients."
    :gallery          {:images ["91566919-324f-4714-9043-f4c24ae2fe98"
                                "caae31b7-36f3-439c-9c14-af0dd83f31e7"
                                "84ecceb8-443a-40fa-936a-6c639bf8280a"
                                "1cc6eff6-308b-4f66-8fa8-98287f56c4e3"
                                "7421e36b-1d41-4747-a6da-b5374a5bfbba"
                                "95c3038d-025d-4185-a607-96356c6867a8"
                                "46510df0-e1b2-45bc-8744-dec585ef165f"
                                "7c7a433f-40e8-4a0d-af74-1b8ff2e69f7e"
                                "f5dd7a95-53c9-4c2f-a380-3698852615e7"
                                "343835b4-1b1d-44a3-9162-a690531f3edd"
                                "08199689-1514-4ae3-bebc-00242dd96f51"]}}
   {:stylist-name     "Angela White"
    :stylist-headshot "a7903783-7c7a-4459-85a7-fc9db361696e"
    :salon-name       "Michae's Hair Salon"
    :salon-address    "5338 Yadkin Rd. Fayetteville, NC"
    :gallery          {:images ["2e6076b8-dcbe-44ba-98b5-3402592d5858"
                                "7c59eea0-952b-4775-a50c-d8b638d6cb48"
                                "b356c268-7624-4ef9-89e3-3f5e065f6511"
                                "2c7cd91a-50ff-4a49-9483-1962714e6493"
                                "ca6f688f-1822-432c-ae28-73160e965a00"
                                "69c46710-3958-4049-bb21-e435f8b33ad9"
                                "7df1ce4b-9e85-4674-968c-dc61bc00c122"]}
    :stylist-bio      "Angela White a.k.a Hairdiva Daplug, is a licensed and versatile stylist with more than 10 years of experience. Discovering her love for all things hair at an early age Angela works with you to achieve your unique look. Specializing in sew-ins, braiding, blow outs, cuts, and coloring."}

   {:stylist-name     "Tamara Johnson"
    :stylist-headshot "be913d9e-e69d-45e9-8a92-23b7dfca01fe"
    :salon-name       "Sara's Hair & Nail Salon"
    :salon-address    "5845C Yadkin Rd. Fayetteville, NC"
    :gallery          {:images ["def0352c-0d85-49cc-904c-f6ced1482cc5"
                                "c6710751-ea7d-4e75-b301-096292550326"
                                "1be01c11-10ee-4b78-8252-0c345c5cf45e"
                                "0820e925-fcd8-440f-bb32-30490e8dd676"
                                "4e5a17f2-f721-49dc-81d7-d495cae08fef"
                                "c2e137cc-a147-4f27-907f-08d13e2c929f"
                                "b9cabcaf-9fcb-4d22-970b-ba3192a17abc"
                                "21fae71d-e749-461f-8f1e-15c242e73f3f"
                                "0a3cd9a0-308b-4de1-a563-553aa88fa66d"]}
    :stylist-bio      "Tamara Johnson, a.k.a. Tamara Nicole brings over 9 years of experience to her chair. A graduate of the Paul Mitchell School she places an emphasis on healthy and strong, shiny hair. Specializing in sew-ins, blowouts, and locs she will make your hair grow!"}

   {:stylist-name     "Valerie Selby"
    :stylist-headshot "f1ba9936-d310-42fb-a0fa-fa54b49e7055"
    :salon-name       "ILLstylz Salon"
    :salon-address    "1016 71st School Rd. Fayetteville, NC"
    :gallery          {:images ["055147ca-5cd9-41c6-8038-18125454c478"
                                "981e5b31-3dc8-4f3e-814a-8899ff78f395"
                                "66e9dd27-5367-4d6b-90f2-a08670f9164b"
                                "cb4b1290-32a0-4a16-b3df-1c214a886201"
                                "94d355f2-fcbb-4d81-824c-9284ee5a993d"
                                "4c16e602-3c11-441a-b035-78a986cb1b9c"
                                "b9048896-2123-43c4-b503-786425ceee9c"
                                "8adaf2dc-3d3e-4045-8549-85bf2b28f71e"
                                "76a6083d-1f14-4660-b70f-17134aae07cc"]}
    :stylist-bio      "As the owner and operator of ILLstylz Salon Valerie backs her love of hair with over 15 years of experience. With a focus on healthy hair a specialty in sew-ins Valerie strives to give your hair what it needs to be healthy regardless of the style or service you choose."}

   {:stylist-name     "Shenee’ Shaw"
    :stylist-headshot "0a03aa7e-7808-4a7a-bf7c-9d352363e1b2"
    :salon-name       "Signature Styles By Shenee'"
    :salon-address    "5338 Yadkin Rd. STE 2 Fayetteville, NC"
    :gallery          {:images ["644f6b35-f7cd-4098-b31b-5ef1865fe1ef"
                                "258770ac-d3a4-4e81-ab5c-a3449b677638"
                                "6a9f7e7f-d820-4714-81c7-ee4efeaa982f"
                                "e0047ea3-f12a-49db-a3b1-f61d2e228168"
                                "70d4ee24-e415-4873-8d9a-132beb864f63"
                                "b96fd795-1bf8-44e7-bdd3-0328428afd5e"
                                "8f9b13a1-879c-4f6a-ab77-c24f03c42577"
                                "94b2f2a1-3246-420c-852a-acd7fe29977f"]}
    :stylist-bio      "Shenee’ is a Master Cosmetologist who brings a wealth of experience with over 24 years of experience. Specializing in sew-ins, she also provides all phases of hair care from natural hair to demi/semi permanent hair color, relaxers, and thermal hair straightening."}

   {:stylist-name     "Tiara Cohen"
    :stylist-headshot "28ea9a60-1254-4325-8593-b6bac09e19e9"
    :salon-name       "Hair Miracles"
    :salon-address    "508 Owen Dr. Suite B Fayetteville, NC"
    :gallery          {:images ["7309ff5a-6a49-4113-bb52-41dc15fcc9b6"
                                "3b5865d5-ca4a-4dcc-98ba-16536b55766f"
                                "729866c4-e578-4e23-ad57-8d5945fc10b4"
                                "baaf45a1-ce35-40f0-a26b-e21802ddd6d2"
                                "9427d213-db31-4f29-a769-f43e3246db96"]}
    :stylist-bio      "With over 22 years of experience Tiara knows that weaves and wigs are a choice that each one of her clients makes, not a must. Starting with a specialty in sew-ins, Tiara has the experience to meet all the needs of her clients dreams."}])

(defn ^:private stylist-slide [selected-index index {:keys [stylist-headshot]}]
  [:div.flex.relative.justify-center.mxn2
   [:div.z5.stacking-context.absolute
    {:style {:margin-top  "14px"
             :margin-left "95px"}}
    (ui/ucare-img {:width "40"} "3cd2b6e9-8470-44c2-ad1f-b1e182d38cb0")]
   (ui/ucare-img {:width "210"
                  :on-click (if (= selected-index index)
                              (utils/send-event-callback events/control-stylist-gallery-open)
                              utils/noop-callback)}
                 stylist-headshot)])

(defn ^:private gallery-slide [ucare-id]
  [:div (ui/aspect-ratio 1 1
                         (ui/ucare-img {:class "col-12"} ucare-id))])

(defn ^:private stylist-info [{:keys [stylist-name salon-name salon-address stylist-bio gallery]} stylist-gallery-open?]
  [:div.py2
   [:div.h3 stylist-name]
   [:div.teal.h6.pt2.bold
    "MAYVENN CERTIFIED STYLIST"]
   [:div.h6.bold.dark-gray salon-name]
   [:div.h6.bold.dark-gray salon-address]
   [:div.h6.dark-gray.pt3.mx-auto.col-12.col-5-on-tb.col-3-on-dt stylist-bio]
   [:div
    [:a.teal.medium.h6.border-teal.border-bottom.border-width-2
     (utils/fake-href events/control-stylist-gallery-open)
     "View Hair Gallery"]

    (when stylist-gallery-open?
      (let [close-attrs (utils/fake-href events/control-stylist-gallery-close)]
        (ui/modal
         {:close-attrs close-attrs
          :col-class "col-12"}
         [:div.relative.mx-auto
          {:style {:max-width "750px"}}
          (component/build carousel/component
                           {:slides   (map gallery-slide (:images gallery))
                            :settings {:slidesToShow 1}}
                           {})
          [:div.absolute
           {:style {:top "1.5rem" :right "1.5rem"}}
           (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                            :close-attrs close-attrs})]])))]])

(defn stylist-details-before-change [prev next]
  (messages/handle-message events/carousel-certified-stylist-slide))

(defn stylist-details-after-change [index]
  (messages/handle-message events/carousel-certified-stylist-index {:index index}))

(defn component
  [{:keys [index sliding? gallery-open?]} owner opts]
  (component/create
   [:div.pt6
    [:div
     (component/build carousel/component
                      {:slides   (map-indexed (partial stylist-slide index) stylist-information)
                       :settings {:swipe         true
                                  :arrows        true
                                  :dots          false
                                  :slidesToShow  5
                                  :centerMode    true
                                  :infinite      true
                                  :focusOnSelect true
                                  :className     "faded-inactive-slides-carousel"
                                  :beforeChange  stylist-details-before-change
                                  :afterChange   stylist-details-after-change
                                  ;; The breakpoints are mobile-last. That is, the
                                  ;; default values apply to the largest screens, and
                                  ;; 1000 means 1000 and below.
                                  :responsive    [{:breakpoint 1200
                                                   :settings   {:slidesToShow 3}}
                                                  {:breakpoint 749
                                                   :settings   {:slidesToShow 1}}]}}
                      {})]
    [:div.center.px6
     [:div {:class (if sliding? "transition-2 transparent" "transition-1 opaque")}
      (stylist-info (get stylist-information index)
                    gallery-open?)]]]))

(defmethod transitions/transition-state events/carousel-certified-stylist-slide [_ _event _args app-state]
  (assoc-in app-state keypaths/carousel-certified-stylist-sliding? true))

(defmethod transitions/transition-state events/carousel-certified-stylist-index [_ _event {:keys [index]} app-state]
  (-> app-state
      (assoc-in keypaths/carousel-certified-stylist-index index)
      (update-in keypaths/carousel dissoc :certified-stylist-sliding?)))

(defmethod transitions/transition-state events/control-stylist-gallery-open [_ _event _args app-state]
  (assoc-in app-state keypaths/carousel-stylist-gallery-open? true))

(defmethod transitions/transition-state events/control-stylist-gallery-close [_ _event _args app-state]
  (update-in app-state keypaths/carousel-stylist-gallery dissoc :open?))
