(ns install.home
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.events.EventType :as EventType]])
            [spice.core :as spice]
            [install.licensed-stylists :as licensed-stylists]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]))

(defn header [text-or-call-number]
  [:div.container.flex.items-center.justify-between.px3.py2
   [:div
    [:img {:src (assets/path "/images/header_logo.svg")
           :style {:height "40px"}}]
    [:div.h7 "Questions? Text or call: "
     (ui/link :link/phone :a.inherit-color {} text-or-call-number)]]
   [:div.col.col-4.h5
    (ui/teal-button (assoc (utils/route-to events/navigate-home)
                           :data-test "shop"
                           :height-class "py2")
                    "Shop")]])

(defn relative-header [{:keys [text-or-call-number]} owner opts]
  (component/create (header text-or-call-number)))

(defn fixed-header [{:keys [text-or-call-number]} owner opts]
  #?(:cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 750 (.-y (goog.dom/getDocumentScroll)))))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props])
         om/IRenderState
         (render-state [this {:keys [show?]}]
           (component/html
            [:div.fixed.top-0.left-0.right-0.z4.bg-white
             (if show?
               {:style {:margin-top "0"}
                :class "transition-2"}
               {:style {:margin-top "-100px"}})
             (header text-or-call-number)]))))
     :clj [:span]))

(defn ^:private stat-block [header content]
  [:div.center.p2 [:div.bold.teal.letter-spacing-1 header]
   [:div.h6.line-height-1 content]])

(defn ^:private as-seen-in-logos [& logo-img-ids]
  (for [{:keys [id width]} logo-img-ids]
    (ui/ucare-img {:class "mx2 my2" :width width} id)))

(defn img-with-circle [diameter img-id content]
  (let [radius (quot diameter 2)]
    [:div
     {:style {:padding-bottom (str radius "px")}}
     [:div.relative
      [:div
       (ui/ucare-img {:class "col-12 col-4-on-tb-dt"} img-id)]
      [:div.bg-teal.border.border-white.border-width-3.circle.absolute.right-0.left-0.mx-auto.flex.items-center.justify-center
       {:style {:height (str diameter "px")
                :width (str diameter "px")
                :bottom (str "-" (- radius 4) "px")}}
       content]]]))

(defn ^:private easy-step [number title copy img-id]
  [:div.py4
   [:div.px6 (img-with-circle 60 img-id [:div.h1.bold.white number])]
   [:div.h3 title]
   [:div.dark-gray.h6 copy]])

(defn ^:private happy-customer [img-id testimony customer-name]
  [:div.p4.flex.items-start.justify-center
   [:div.col-5
    (ui/ucare-img {:class "col-12"} img-id)]
   [:div.px2.flex.flex-column
    [:div.line-height-3.h6 \“ testimony \”]
    [:div.h6.bold "- "  customer-name]]])

(defn ^:private component
  [{:keys [header carousel-certified-stylist]} owner opts]
  (component/create
   [:div
    (component/build relative-header header nil)
    (component/build fixed-header header nil)

    [:div.bg-cover.bg-top.bg-free-install-landing.col-12.p4
     [:div.teal.h1.shadow.bold.pt2.shout "free install"]
     [:div.medium.letter-spacing-1.col-7.h3.white.shadow "Get your Mayvenn hair installed for FREE by some of the best stylists in Fayetteville, NC"]]

    [:div.flex.items-center.justify-center.p1.pt2.pb3
     (stat-block "100,000+" "Mayvenn Stylists Nationwide")
     (stat-block "200,000+" "Happy Mayvenn Customers")
     (stat-block "100%" "Guaranteed Human Hair")]

    [:div.col-12.bg-gray.py2
     [:div.dark-gray.col-12.center.h7.medium.letter-spacing-4.p1 "AS SEEN IN"]
     (into [:div.flex.flex-wrap.justify-around.items-center]
           (as-seen-in-logos
            {:id "a2e763ea-1837-43fd-8531-440d18360e1e" :width "160"}
            {:id "74f56834-b879-415a-9e55-87a059767297" :width "75"}
            {:id "b1a3d9c1-80a0-4549-9603-36fb65b5bebb" :width "56"}
            {:id "4f8c1a9d-ab71-4881-97df-b4a724354faa" :width "45"}
            {:id "3428dfc2-bc0a-40f2-9bdd-c79df6abd63f" :width "150"}))]

    [:div.border.border-teal.border-width-2.m3.center.p3
     [:div
      [:div.py2
       [:div.teal.letter-spacing-6.medium.h6 "3 EASY STEPS"]
       [:div.h2 "Get a FREE install"]]
      [:div.h6 "Purchase 3 bundles of Mayvenn hair and your install by a Mayvenn Certified Stylist is FREE!"]]
     (easy-step 1
                "Buy 3 bundles or more"
                "Closures and fronts count, too! Our hair is 100% human, backed by a 30 day guarantee and starts at $30 per bundle."
                "fdcc8acc-443c-4b2f-b510-0d940297f997")
     (easy-step 2
                "A Fayetteville, NC exclusive offer"
                "Your Mayvenn order must be shipped to a qualified address in Fayetteville, NC."
                "6263c536-f548-45dc-ba89-ca68ad7c44c8")
     (easy-step 3
                "Book your FREE install"
                "After completing your purchase, Mayvell will contact you to arrange your FREE install appointment with a Mayvenn Certified Stylists."
                "52dcdffb-cc44-4f80-88c8-325de7c3fa62")]

    [:div.bg-transparent-teal.py6
     [:div.pt4.teal.letter-spacing-6.bold.center.h6 "HAPPY CUSTOMERS"]
     [:div.px3
      (happy-customer "dd6b26ed-1f15-437c-ac2c-e289d3a854fe"
                      "I freaking love Mayvenn hair, like they are BOMB.COM GIRL! Yass!"
                      "J Luxe")
      (happy-customer "014856c8-7fa5-40b7-a707-48361c37f04f"
                      "I'm 100% satisfied, that's all I can say, I'm 100% satisfied"
                      "Cara Scott")
      (happy-customer "b7258cae-1aac-4755-9f61-90e9908ff7a7"
                      "Ugh God you guys, like you don't understand, I love this hair."
                      "Tiona Chantel")]]

    (component/build licensed-stylists/component carousel-certified-stylist {})]))

(def ^:private certified-stylists
  [{:stylist-name     "Aundria Carter"
    :stylist-headshot "63acc2ac-43cc-48cb-9db7-0361f01aaa25"
    :salon-name       "Giovani Hair Salon"
    :salon-address    "520 S Reilly Rd. Fayetteville, NC"
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

   {:stylist-name     "Demetria Murphy"
    :stylist-headshot "2f98fa6e-321b-4d5c-993b-2f424cb221c0"
    :salon-name       "Exclusive Hair Design"
    :salon-address    "224 McPherson Church Rd. Fayetteville, NC"
    :gallery          {:images ["e863f9a9-e549-45ba-a49f-4df7a9ad45bc"
                                "878286ba-2be0-48f0-941c-c56d799cda94"
                                "0fe1dd1c-19c7-434b-b56d-f125027d9193"
                                "ea7b6e03-ad8c-4c6a-a6cc-29c3f4ddcb2e"
                                "9626fcc8-ff4b-4d10-96c6-bcf6eb97094c"
                                "75042965-0d9a-4a2c-bfb5-dc26acc50578"
                                "439ca881-7410-4418-9eaa-8c36a1def1b8"]}
    :stylist-bio      "Demetria, AKA MizDee, has been doing what she loves as a licensed stylist for over 15 years taking pride in transforming her clients into their greater self. Specializing in sew-ins, cuts, and styling she strives to ensure each one of her clients have a memorable experience."}

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

(defn ^:private query [data]
  {:header                     {:text-or-call-number "1-310-733-0284"}
   :carousel-certified-stylist {:carousel-certified-stylist-index    (get-in data keypaths/carousel-certified-stylist-index)
                                :carousel-certified-stylist-sliding? (get-in data keypaths/carousel-certified-stylist-sliding?)
                                :stylist-gallery-open?               (get-in data keypaths/carousel-stylist-gallery-open?)
                                :stylists                            certified-stylists}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
