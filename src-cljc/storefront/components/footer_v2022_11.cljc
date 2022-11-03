(ns storefront.components.footer-v2022-11
  (:require [spice.date :as date]
            [storefront.component :as c]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [mayvenn.visual.tools :as vt]
            [homepage.ui.email-capture :as email-capture]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.platform.component-utils :as util]
            [storefront.components.accordions.product-info :as product-info]))

(c/defcomponent info-accordion-drawer-links-list [{:keys [links row-count column-count]} _ _]
  [:div.p4.grid.gap-4
   {:style {:grid-template-columns (str "repeat(" column-count ", 1fr)")
            :grid-template-rows    (str "repeat(" row-count ", 1fr)")
            :grid-auto-flow        "column"}}
   (map (fn [{:keys [target url copy]}]
          [:a.inherit-color
           (merge
            (when target (apply util/route-to target))
            (when url {:href url}))
           copy]) links)])

(defn ^:private contact-item
  [icon title copy]
  [:div.flex-auto.flex.flex-column.items-center.justify-end
   icon
   [:div.proxima.content-4.pt2 title]
   [:div.proxima.content-2 copy]])

(c/defcomponent contact-block
  [{:keys []} _ _]
  [:div.p4
   [:div.proxima.content-4
    "Have a problem? Need advice on a style or product? Here are a few ways to get a hold of us."]
   [:div.grid.my5
    {:style {:grid-template-columns "1fr auto 1fr"
             :grid-template-rows    "1fr 1fr"
             :row-gap               "40px"}}
    (contact-item (svg/customer-service-representative {:style {:height "2.0em"}})
                  "Monday to Friday"
                  "11am to 8pm ET")
    [:div.border]
    (contact-item (svg/phone-ringing {})
                  "Call Us"
                  (ui/link :link/phone :a.inherit-color {} "+1 (888) 562-7952"))
    (contact-item (svg/message-bubble {})
                  "Text"
                  (ui/link :link/sms :a.inherit-color {} "34649"))
    [:div.border]
    (contact-item (svg/mail-envelope {})
                  "email"
                  (ui/link :link/email :a.inherit-color {} "help@mayvenn.com"))]
   [:div.proxima.content-4.dark-gray
    "* Message & data rates may apply. Message frequency varies. See "
    [:a (utils/route-to e/navigate-content-tos) "terms"]
    " & "
    [:a (utils/route-to e/navigate-content-privacy) "privacy policy"]
    "."]])

(c/defcomponent our-locations
  [{:keys []} _ _]
  [:div.p4
   (ui/img {:src             "//ucarecdn.com/9d736154-7ec4-4414-9e60-ca4f515d7e55/"
            :picture-classes "container-size"})
   [:div.grid.my5.items-center
    {:style {:grid-template-columns "1fr auto 1fr"
             :row-gap               "40px"}}
    [:div.flex.flex-column
     [:div.flex-auto.flex.flex-column.items-center.justify-end
      [:div.proxima.content-4.pt2 "Monday to Friday"]
      [:div.proxima.content-2 "11am to 7pm ET"]]
     [:div.flex-auto.flex.flex-column.items-center.justify-end
      [:div.proxima.content-4.pt2 "Sunday"]
      [:div.proxima.content-2 "12pm-6pm"]]]
    [:div.border.container-size]
    [:div.flex.flex-column.my4.gap-4.items-center
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart-mansfield) "Mansfield, TX"]
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart-katy) "Katy, TX"]
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart-dallas) "Dallas, TX"]
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart-houston) "Houston, TX"]
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart-grand-prairie) "Grand Prairie, TX"]
     [:a.inherit-color (utils/route-to e/navigate-retail-walmart) "See all locations"]]]])

(c/defcomponent info-accordion-face-open [{:keys [copy]} _ _]
  [:div.shout.content-3.p4.bold copy])
(c/defcomponent info-accordion-face-closed [{:keys [copy]} _ _]
  [:div.shout.content-3.p4 copy])
(c/defcomponent info-accordion-contents [{:info-accordion.contents/keys [type] :as data} _ _]
  [:div.bg-pale-purple
   (case type
     :faq-accordion (c/build accordion/component
                             (vt/with :footer-faq data)
                             {:opts {:accordion.drawer.open/face-component   product-info/question-open
                                     :accordion.drawer.closed/face-component product-info/question-closed
                                     :accordion.drawer/contents-component    product-info/answer}})
     :links-list    (c/build info-accordion-drawer-links-list data)
     :contact-us    (c/build contact-block data)
     :our-locations (c/build our-locations data)
     [:div.bg-red "CONTENT MISSING!"])])

(defn- social-link
  ([uri icon] (social-link {:height "20px" :width "20px"} uri icon))
  ([{:keys [height width]} uri icon]
   (c/html
    ;; https://web.dev/external-anchors-use-rel-noopener/
    [:a.block.mr4 {:href uri :rel "noopener" :target "_blank"}
     [:div {:style {:width width :height height}}
      ^:inline icon]])))

(c/defcomponent social-media-block
  [_ _ _]
  [:div.px4.py3.flex.items-center
   ^:inline (social-link {:height "28px" :width "28px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter {:class "fill-p-color"}))
   ^:inline (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram {:class "fill-p-color"}))
   ^:inline (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook {:class "fill-p-color"}))
   ^:inline (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest {:class "fill-p-color"}))])

(c/defcomponent essence-block
  [{:keys [copy]} owner opts]
  [:div.px4.pb3.proxima.content-4.dark-dark-gray copy])

(defn ^:private underfoot-link [opts label]
  (c/html [:a.block.inherit-color.my2 opts label]))

(c/defcomponent underfoot
  [{:keys [] :as data} owner opts]
  [:div.white.bg-black.px4.py5.proxima.content-4
   ^:inline (svg/mayvenn-text-logo {:height "29px"
                                    :width  "115px"
                                    :class  "fill-white"})
   [:div.flex.justify-between.my3
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-privacy)
                                    :data-test "content-privacy") "Privacy")
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#ca-privacy-rights")}
                             "CA Privacy Rights")
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-tos)
                                    :data-test "content-tos") "Terms")
    ;; use traditional page load so anchors work
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#our-ads")} "Our Ads")]
   [:div.flex.items-center {:key "minimal"}
    "©" (date/year (date/now)) " " "Mayvenn"]])

(defn query
  [app-state]
  (merge
   (let [textfield-keypath k/footer-email-value
         email             (get-in app-state textfield-keypath)
         submitted?        (get-in app-state k/footer-email-submitted)]
     (vt/within :email-capture
                {:submit/target             [e/control-footer-email-submit {:email email}]
                 :text-field/id             "footer-email-capture-input"
                 :text-field/placeholder    "Enter your Email"
                 :text-field/focused        (get-in app-state k/ui-focus)
                 :text-field/keypath        textfield-keypath
                 :text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                 :text-field/email          email
                 :text-field/submitted-text (when submitted? "Thank you for subscribing.")}))
   {:essence-block/copy "Included is a one year subscription to ESSENCE Magazine - a $10 value! Offer and refund details will be included with your confirmation."}
   (accordion/accordion-query
    {:id                :info-accordion
     :allow-all-closed? true
     :allow-multi-open? true
     :open-drawers      (:accordion/open-drawers (accordion/<- app-state :info-accordion))
     :drawers           (remove nil?
                                [{:id       "faq"
                                  :face     {:copy "FAQs"}
                                  :contents (merge {:info-accordion.contents/type :faq-accordion}
                                                   (accordion/accordion-query
                                                    {:id                :footer-faq
                                                     :allow-all-closed? true
                                                     :allow-multi-open? false
                                                     :open-drawers      (-> app-state (accordion/<- :footer-faq) :accordion/open-drawers)
                                                     :drawers           (map-indexed (fn [ix {:keys [question answer]}]
                                                                                       {:id       (str "footer-faq-" ix)
                                                                                        :face     {:copy (:text question)}
                                                                                        :contents {:answer answer}})
                                                                                     (-> app-state (get-in (conj k/cms-faq :sitewide-footer)) :question-answers))}))}
                                 {:id       "shop"
                                  :face     {:copy "Shop"}
                                  :contents {:info-accordion.contents/type :links-list
                                             :row-count                    9
                                             :column-count                 2
                                             :links                        [{:target [e/navigate-category
                                                                                      {:page/slug "wigs" :catalog/category-id "13"}]
                                                                             :copy   "Wigs"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "human-hair-bundles" :catalog/category-id "27"}]
                                                                             :copy   "Hair Bundles"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "virgin-body-wave" :catalog/category-id "5"}]
                                                                             :copy   "Virgin Body Wave"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "virgin-loose-wave" :catalog/category-id "6"}]
                                                                             :copy   "Virgin Loose Wave"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "virgin-deep-wave" :catalog/category-id "8"}]
                                                                             :copy   "Virgin Deep Wave"}
                                                                            {:copy "Dyed Virgin Hair [LINK TARGET MISSING]"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "hair-extensions" :catalog/category-id "28"}]
                                                                             :copy   "Hair Extensions"}
                                                                            {:target [e/navigate-shop-by-look {:album-keyword :straight-bundle-sets}]
                                                                             :copy   "Straight Bundle Sets"}
                                                                            {:target [e/navigate-shop-by-look {:album-keyword :wavy-curly-bundle-sets}]
                                                                             :copy   "Wavy Curly Bundle Sets"}
                                                                            {:copy "All Blonde Hair [LINK TARGET MISSING]"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "360-frontals" :catalog/category-id "10"}]
                                                                             :copy   "Virgin 360 Frontals"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "virgin-lace-frontals" :catalog/category-id "29"}]
                                                                             :copy   "Virgin Frontals"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "seamless-clip-ins" :catalog/category-id "21"}]
                                                                             :copy   "Clip-Ins"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "taps-ins" :catalog/category-id "22"}]
                                                                             :copy   "Tape-Ins"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug "wigs" :catalog/category-id "48"}]
                                                                             :copy   "Sale"}]}}
                                 {:id       "about"
                                  :face     {:copy "About"}
                                  :contents {:info-accordion.contents/type :links-list
                                             :row-count                    4
                                             :column-count                 1
                                             :links                        [{:target [e/navigate-content-about-us]
                                                                             :copy   "Our Story"}
                                                                            {:copy "Events [LINK TARGET MISSING]"}
                                                                            {:url  "https://shop.mayvenn.com/blog/"
                                                                             :copy "Blog"}
                                                                            {:url "https://jobs.mayvenn.com/"
                                                                             :copy "Careers"}]}}
                                 {:id       "our-locations"
                                  :face     {:copy "Our Locations"}
                                  :contents {:info-accordion.contents/type :our-locations}}
                                 {:id       "contact-us"
                                  :face     {:copy "Contact Us"}
                                  :contents {:info-accordion.contents/type :contact-us}}])})))

(c/defcomponent component
  [{:keys [] :as data} owner opts]
  [:div
   (c/build email-capture/organism data) ; TODO: use with/within to avoid rerenders?
   (c/build layered/lp-divider-purple-pink)
   (c/build accordion/component
            (vt/with :info-accordion data)
            {:opts
             (vt/within :accordion.drawer
                        {:open/face-component   info-accordion-face-open
                         :closed/face-component info-accordion-face-closed
                         :contents-component    info-accordion-contents})})
   (c/build social-media-block)
   (c/build essence-block (vt/with :essence-block data))
   (c/build underfoot (vt/with :underfoot data) opts)])
