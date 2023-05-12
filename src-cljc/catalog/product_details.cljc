(ns catalog.product-details
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.style]
                       [storefront.accessors.auth :as auth]
                       [storefront.api :as api]
                       [storefront.browser.scroll :as scroll]
                       [storefront.components.popup :as popup]
                       [storefront.components.svg :as svg]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.google-analytics :as google-analytics]
                       [storefront.hooks.quadpay :as zip]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.hooks.seo :as seo]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select ?wig]]
            api.current
            api.orders
            api.products
            [catalog.cms-dynamic-content :as cms-dynamic-content]
            [catalog.facets :as facets]
            catalog.keypaths
            [catalog.product-details-ugc :as ugc]
            [catalog.products :as products]
            [catalog.selector.sku :as sku-selector]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.pre-accordion :as pre-accordion]
            [catalog.ui.molecules :as catalog.M]
            [catalog.ui.popup-shade-finder :as popup-shade-finder]
            [catalog.reviews :as reviews]
            [clojure.string]
            [homepage.ui.faq :as faq]
            [markdown-to-hiccup.core :as markdown]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.sites :as sites]
            [storefront.component :as c]
            [storefront.components.accordions.product-info :as accordions.product-info]
            [storefront.components.carousel :as carousel-neue]
            [storefront.components.money-formatters :as mf]
            [storefront.components.accordion-v2022-10 :as accordion-neue]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}] 
            [storefront.routes :as routes]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            storefront.ugc
            [spice.core :as spice]
            [spice.maps]))

(defn two-column-layout [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    wide-left]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(def sold-out-button
  [:div.pt1.pb3.px3
   (ui/button-large-primary {:on-click  utils/noop-callback
                             :data-test "sold-out"
                             :disabled? true}
                            "Sold Out")])

(def unavailable-button
  [:div.pt1.pb3.px3
   (ui/button-large-primary {:on-click  utils/noop-callback
                             :data-test "unavailable"
                             :disabled? true}
                            "Unavailable")])

(def shipping-and-guarantee
  (c/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn image-body [i {:keys [url alt]}]
  (ui/aspect-ratio
   640 580
   (if (zero? i)
     (ui/img {:src url :class "col-12" :width "100%" :alt alt})
     (ui/defer-ucare-img
       {:class       "col-12"
        :alt         alt
        :width       640
        :placeholder (ui/large-spinner {:style {:height     "60px"
                                                :margin-top "130px"}})}
       url))))

(defn carousel [images _]
  (c/build carousel/component
           {:images images}
           {:opts {:settings {:edgePadding 0
                              :items       1}
                   :slides   (map-indexed image-body images)}}))

(defn ^:private get-selected-options [selections options]
  (reduce
   (fn [acc selection]
     (assoc acc selection
            (first (filter #(= (selection selections) (:option/slug %))
                           (selection options)))))
   {}
   (keys selections)))

(c/defcomponent product-summary
  "Displays basic information about a particular product"
  [data _ _]
  [:div.mt3.mx3
   [:h1.flex-auto
    (titles/proxima-left (with :title data))]
   [:div.flex.justify-between.my2
    (c/build reviews/summary (with :reviews.summary data))
    [:div.col-3 (catalog.M/price-block data)]]
   #?(:cljs
      (c/build zip/pdp-component data _))])

(defn diamond-swatch [ucare-id facet-slug option-slug option-name selected? target size]
  (let [container-width #?(:clj (.hypot java.lang.Math size size)
                           :cljs (js/Math.hypot size size))]
    [(if target :a :div)
     (merge {:style {:width (str container-width "px")
                     :height (str container-width "px")}
             :class (str "flex items-center justify-center"
                         (when target " inherit-color pointer"))
             :key   option-slug}
            (when target
              {:data-test (str "picker-" facet-slug "-" option-slug)})
            (when target
              (apply utils/fake-href target)))
     [:div.overflow-hidden.flex.items-center.justify-center
      {:style {:transform "rotate(45deg)"
               :width     (str size "px")
               :height    (str size "px")
               :padding   "0"}
       :class (when selected? "border border-width-2 border-s-color")}
      [:img
       {:key   (str "product-details-" option-name "-" option-slug)
        :style {:transform "rotate(-45deg)"
                :width     (str (inc container-width) "px")
                :height    (str (inc container-width) "px")}
        :alt   option-name
        :src   (str "https://ucarecdn.com/" (ui/ucare-img-id ucare-id) "/-/format/auto/")}]]]))

(c/defcomponent picker-accordion-face-open
  [{:keys [facet-name facet-slug swatch option-slug option-name]} _ _]
  [:div.grid.ml2.py3.items-center
   {:data-test (str "picker-" facet-slug "-open")
    :style {:grid-template-columns "4rem auto"}}
   [:div.shout.content-3.bold facet-name]
   [:div.flex.items-center.gap-2
    {:data-test (str "picker-selected-" facet-slug "-" option-slug)}
    (when swatch
      (diamond-swatch swatch facet-slug option-slug option-name false nil 20))
    option-name]])

(c/defcomponent picker-accordion-face-closed
  [{:keys [facet-name facet-slug swatch option-slug option-name]} _ _]
  [:div.grid.ml2.py3.items-center
   {:data-test (str "picker-" facet-slug "-closed")
    :style {:grid-template-columns "4rem auto"}}
   [:div.shout.content-3 facet-name]
   [:div.flex.items-center.gap-2
    {:data-test (str "picker-selected-" facet-slug "-" option-slug)}
    (when swatch
      (diamond-swatch swatch facet-slug option-slug option-name false nil 20))
    option-name]])

(c/defcomponent picker-accordion-contents
  [{:keys [facet swatches? options link-text link-message] :as picker-contents} _ _]
  [:div.p2
   {:key (str "picker-contents-" facet)
    :data-test (str "picker-contents-" facet)}
   [:div.flex.flex-wrap.gap-2
    (if swatches?
      (for [{:keys [option-slug selected? option-name rectangle-swatch target]} options]
        (diamond-swatch rectangle-swatch facet option-slug option-name selected? target 30))
      (for [{:keys [option-slug copy selected? target]} options]
        [(if target :a :div)
         (merge {:key   option-slug
                 :style {:width  "2.5rem"
                         :height "2.5rem"}
                 :class (str "border flex items-center justify-center"
                             (if selected?
                               " border-s-color border-width-2 bg-refresh-gray"
                               " border-gray")
                             (when target
                               " inherit-color pointer"))}
                (when target
                  {:data-test (str "picker-" facet "-" option-slug)})
                (when target
                  (apply utils/fake-href target)))
         copy]))]
   (when-let [{:keys [target id content]} (not-empty (with :link picker-contents))]
     [:div.right-align
      (ui/button-small-underline-primary
       (assoc (apply utils/fake-href target) :data-test id)
       content)])])

(c/defcomponent loading-template
  [_ _ _]
  [:div.flex.h2.p1.m1.items-center.justify-center
   {:style {:height "25em"}}
   (ui/large-spinner {:style {:height "4em"}})])

(c/defcomponent template
  [{:keys [carousel-images
           product
           selected-sku
           ugc
           faq-section
           add-to-cart] :as data}
   _
   opts]
  (let [unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (c/html
     [:div
      [:div.container.pdp-on-tb
       #?(:cljs (popup/built-component data nil))
       (when (:offset ugc)
         [:div.absolute.overlay.z4.overflow-auto
          {:key "popup-ugc"}
          (c/build ugc/popup-component (assoc ugc :id "popup-ugc") opts)])
       [:div
        {:key "page"}
        (two-column-layout
         (c/html
          (if (seq (with :product-carousel data))
            (c/build carousel-neue/component
                     (with :product-carousel data)
                     {:opts {:carousel/exhibit-thumbnail-component carousel-neue/product-carousel-thumbnail
                             :carousel/exhibit-highlight-component carousel-neue/product-carousel-highlight
                             :carousel/id                          :product-carousel}})
            [:div ^:inline
             (carousel carousel-images product)
             #_
             (c/build ugc/component (assoc ugc :id "ugc-dt") opts)]))
         (c/html
          [:div
           (c/build product-summary data)
           (c/build accordion-neue/component
                    (with :pdp-picker data)
                    {:opts {:accordion.drawer.open/face-component   picker-accordion-face-open
                            :accordion.drawer.closed/face-component picker-accordion-face-closed
                            :accordion.drawer/contents-component    picker-accordion-contents}})
           [:div.mt4
            (cond
              unavailable? unavailable-button
              sold-out?    sold-out-button
              :else        (c/build add-to-cart/organism add-to-cart))]
           (when (products/stylist-only? product)
             shipping-and-guarantee)
           (c/build pre-accordion/component (with :pre-accordion data)) 
           (c/build accordion-neue/component
                    (with :info-accordion data)
                    {:opts {:accordion.drawer.open/face-component   accordions.product-info/face-open
                            :accordion.drawer.closed/face-component accordions.product-info/face-closed
                            :accordion.drawer/contents-component    accordions.product-info/contents}})
           [:div.hide-on-tb-dt.m3
            [:div.mxn2.mb3 (c/build ugc/component (assoc ugc :id "ugc-mb") opts)]]]))]]
      (when (seq (with :reviews.browser data))
        [:div.container.col-7-on-tb-dt.px2
         (c/build reviews/browser (with :reviews.browser data))])
      (when faq-section
        [:div.container
         (c/build faq/organism faq-section opts)])])))

(defn ugc-query [product sku data]
  (let [shop?              (= :shop (sites/determine-site data))
        album-keyword      (storefront.ugc/product->album-keyword shop? product)
        cms-ugc-collection (get-in data (conj keypaths/cms-ugc-collection album-keyword))]
    (when-let [social-cards (when product
                              (->> cms-ugc-collection
                                   :looks
                                   (mapv (partial contentful/look->pdp-social-card album-keyword))
                                   not-empty))]
      {:carousel-data {:product-id   (:catalog/product-id product)
                       :product-name (:copy/title product)
                       :page-slug    (:page/slug product)
                       :sku-id       (:catalog/sku-id sku)
                       :social-cards social-cards}
       :offset        (:offset (get-in data keypaths/navigation-query-params))
       :close-message [events/navigate-product-details
                       {:catalog/product-id (:catalog/product-id product)
                        :page/slug          (:page/slug product)
                        :query-params       {:SKU (:catalog/sku-id sku)}}]})))

(defn ^:private get-essential-critera-from-skuer [skuer]
  (select-keys skuer (:selector/essentials skuer)))

(defn find-carousel-images [product product-skus images-catalog selections selected-sku]
  (->> (selector/match-all {}
                           (or (not-empty (get-essential-critera-from-skuer selected-sku))
                               (not-empty selections)
                               (not-empty (get-essential-critera-from-skuer (first product-skus))))
                           (images/for-skuer images-catalog product))
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn default-selections
  "Using map of current selections (which can be empty)
  for un-selected values picks first option from the available options.
  e.g.: if there's no `hair/color` in `selections` map - it sets it to whatever the first in the list, e.g.: \"black\""
  [facets product product-skus images]
  (let [options (sku-selector/product-options facets product product-skus images)]
    ;; PERF(jeff): can use transients here
    (reduce
     (fn [a k]
       (if (get a k)
         a
         (assoc a k (-> options k first :option/slug))))
     {}
     (keys options))))

#?(:cljs
   [(defmethod popup/query :length-guide [state]
      (when-let [image (get-in state catalog.keypaths/length-guide-image)]
        {:length-guide/image-url    (:url image)
         :length-guide/close-target [events/popup-hide-length-guide]
         :length-guide/primary      "Length Guide"}))
    (defmethod popup/component :length-guide
      [{:length-guide/keys [image-url close-target primary]} _ _]
      (ui/modal
       {:close-attrs
        {:on-click #(apply messages/handle-message close-target)}}
       [:div.bg-white
        [:div.bg-white.col-12.flex.justify-between.items-center
         [:div {:style {:min-width "42px"}}]
         [:div primary]
         [:a.p3
          (merge (apply utils/fake-href close-target)
                 {:data-test "close-length-guide-modal"})
          (svg/x-sharp {:style {:width  "12px"
                                :height "12px"}})]]
        (ui/aspect-ratio 1.105 1
                         (ui/img
                          {:class    "col-12"
                           :max-size 600
                           :src      image-url}))]))])

(defmethod transitions/transition-state events/popup-show-length-guide
  [_ _ {:keys [length-guide-image]} state]
  (-> state
      (assoc-in keypaths/popup :length-guide)
      (assoc-in catalog.keypaths/length-guide-image length-guide-image)))

#?(:cljs
   (defmethod trackings/perform-track events/popup-show-length-guide
     [_ _ {:keys [location]} _]
     (stringer/track-event "length_guide_link_pressed"
                           {:location location})))

#?(:cljs
   [(defmethod popup/query :return-policy [state]
      {:return-policy/close-target [events/popup-hide-return-policy]
       :return-policy/primary      "Return & Exchange Policy"
       :return-policy/contact-number "1-888-562-7952"})
    (defmethod popup/component :return-policy
      [{:return-policy/keys [close-target primary contact-number]} _ _]
      (ui/modal
       {:close-attrs
        {:on-click #(apply publish close-target)}}
       [:div.bg-white.stretch.p4
        [:div.bg-white.col-12.flex.justify-between.items-center
         [:div.content-1 primary]
         [:a.p3
          (merge (apply utils/fake-href close-target)
                 {:data-test "close-return-policy-modal"})
          (svg/x-sharp {:style {:width  "12px"
                                :height "12px"}})]]
        [:p "We offer 30-day refunds and exchanges on all of our products,
        except for custom and bespoke units. Custom and bespoke units are
        only eligible for exchange."]
        [:br]
        [:p.bold "Exchange Policy:"]
        [:p "Wear it, dye it, even cut it. If you’re not in love with your hair, we’ll exchange it within 30 days."]
        [:br]
        [:p "This means that even if your hair has been previously worn,
        installed, cut, colored, or styled, we will still exchange it for you!"]
        [:br]
        [:p.bold "Refund Policy:"]
        [:p "If you are not completely satisfied with your purchase, we will
        refund your payment if the item is returned in its original condition
        within 30 days."]
        [:br]
        [:p "\"Original condition\" means that the item has not been altered,
        washed, or used in any way. Additionally, bundles must not be unraveled
        to be eligible for a full refund. Wigs, closures, and frontals must have
        the lace and any straps intact (the lace has not been cut or colored,
        and the straps have not been removed)." ]
        [:br]
        [:p "We recommend that you confirm that you have received the correct texture, length, and other features before unwrapping and installing your hair. This will help ensure that your purchase is eligible for a full refund if you decide to switch it up."]
        [:br]
        [:p "Contact our Customer Service team at "
         (ui/link :link/phone :a.black.bold {} contact-number)
         " from 8am-5pm PST, Monday through Friday or email us at "
         (ui/link :link/email :a.black.bold {} "help@mayvenn.com")
         "."]]))])

(defmethod transitions/transition-state events/popup-show-return-policy
  [_ _ {:keys []} state]
  (-> state
      (assoc-in keypaths/popup :return-policy)))

#?(:cljs
   (defmethod trackings/perform-track events/popup-show-return-policy
     [_ _ tracking-data _]
     (stringer/track-event "return_policy_link_pressed"
                           tracking-data)))

(defn shipping-delay-hack
  [{:shipping-delay/keys [show?]}]
  (when show?
    [:div.bg-warning-yellow.border.border-warning-yellow.my3
     [:div.bg-lighten-4.p3
      [:span.bold "Shipping Delay: "]
      [:span "There is a slight delay in shipping for 1 or more products in your cart. Ships by Monday (5/1). We apologize for any inconvenience."]]]))

#?(:cljs
   [(defmethod popup/query :shipping-options [state]
      {:shipping-options/close-target   [events/popup-hide-shipping-options]
       :shipping-options/primary        "Shipping"
       :shipping-delay/show?            (:show-shipping-delay (get-in state keypaths/features))
       :shipping-options/drop-shipping? (->> [(get-in state catalog.keypaths/detailed-product-selected-sku)]
                                             (select {:warehouse/slug #{"factory-cn"}})
                                             boolean)})
    (defmethod popup/component :shipping-options
      [{:shipping-options/keys [close-target primary drop-shipping?] :as data} _ _]
      (ui/modal
       {:close-attrs
        {:on-click #(apply publish close-target)}}
       [:div.bg-white.stretch.p4
        [:div.bg-white.col-12.flex.justify-between.items-center.py3
         [:div.content-1 primary]
         [:a.p3
          (merge (apply utils/fake-href close-target)
                 {:data-test "close-return-policy-modal"})
          (svg/x-sharp {:style {:width  "12px"
                                :height "12px"}})]]
        (if drop-shipping?
          [:div.grid.gap-2
           [:div.grid.grid-cols-3.gap-2
            [:div "Option"]
            [:div "Time"]
            [:div "Cost"]]
           [:p.col-12.border-bottom.border-width-2]
           [:div.grid.grid-cols-3.gap-2.border-bottom.border-warm-gray.pb1
            [:div.content-3 "Standard"]
            [:div.content-3 "7-10 business days"]
            [:div.content-3 "Free"]]]
          [:div
           [:div.grid.gap-2
            [:div.grid.grid-cols-3.gap-2
             [:div "Option"]
             [:div "Time"]
             [:div "Cost"]]
            [:p.col-12.border-bottom.border-width-2]
            [:div.grid.grid-cols-3.gap-2.border-bottom.border-warm-gray.pb1
             [:div.content-3 "Standard"]
             [:div.content-3 "4-6 business days"]
             [:div.content-3 "Free"]]
            [:div.grid.grid-cols-3.gap-2.border-bottom.border-warm-gray.pb1
             [:div.content-3 "Priority (USPS)"]
             [:div.content-3 "2-4 business days"]
             [:div.content-3 "$2.99"]]
            [:div.grid.grid-cols-3.gap-2.border-bottom.border-warm-gray.pb1
             [:div.content-3 "Express (FedEx)"]
             [:div.content-3 "1-2 business days"]
             [:div.content-3 "$20.00"]]
            [:div.grid.grid-cols-3.gap-2.border-bottom.border-warm-gray.pb1
             [:div.content-3 "Rush (Overnight - FedEx)"]
             [:div.content-3 "1 business days"]
             [:div.content-3 "$40.00"]]]
           (shipping-delay-hack data)])]))])

(defmethod transitions/transition-state events/popup-show-shipping-options
  [_ _ {:keys []} state]
  (-> state
      (assoc-in keypaths/popup :shipping-options)))

#?(:cljs
   (defmethod trackings/perform-track events/popup-show-shipping-options
     [_ _ tracking-data _]
     (stringer/track-event "shipping_options_link_pressed"
                           tracking-data)))

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn determine-sku-from-selections
  [app-state new-selections]
  (let [product-id     (get-in app-state catalog.keypaths/detailed-product-id)
        product        (products/product-by-id app-state product-id)
        product-skus   (products/extract-product-skus app-state product)
        old-selections (get-in app-state catalog.keypaths/detailed-product-selections)]
    (->> product-skus
         (selector/match-all {}
                             (merge old-selections new-selections))
         first-when-only)))

(defn add-to-cart-query
  [app-state]
  (let [selected-sku    (get-in app-state catalog.keypaths/detailed-product-selected-sku)]
    (merge
     {:cta/id    "add-to-cart"
      :cta/label "Add to Bag"

      ;; Fork here to use bulk add to cart
      :cta/target                [events/control-add-sku-to-bag
                                  {:sku      selected-sku
                                   :quantity (get-in app-state keypaths/browse-sku-quantity 1)}]
      :cta/spinning?             (utils/requesting? app-state (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
      :cta/disabled?             (not (:inventory/in-stock? selected-sku))
      :shipping-delay/show?      (and (:show-shipping-delay (get-in app-state keypaths/features))
                                      (->> [(get-in app-state catalog.keypaths/detailed-product-selected-sku)]
                                           (select {:warehouse/slug #{"factory-cn"}})
                                           boolean
                                           not))
      :sub-cta/promises          (if (:show-return-and-shipping-modals (get-in app-state keypaths/features))
                                   [{:icon :svg/shield
                                     :copy "Not the perfect match? We'll exchange it within 30 days."
                                     :promise-target [events/popup-show-return-policy {:product (products/current-product app-state)
                                                                                       :location "pdp"}]
                                     :promise-target-copy "Return & Exchange Policy"}
                                    {:icon :svg/ship-truck
                                     :copy "Free standard shipping on all orders."
                                     :promise-target [events/popup-show-shipping-options {:product (products/current-product app-state)
                                                                                          :location "pdp"}]
                                     :promise-target-copy "Shipping Options"}
                                    {:icon :svg/market
                                     :copy "Try it on at one of our Texas locations."}]
                                   [{:icon :svg/shield
                                     :copy "30 day guarantee"}
                                    {:icon :svg/ship-truck
                                     :copy "Free standard shipping"}
                                    {:icon :svg/market
                                     :copy "Come visit our Texas locations"}])
      :sub-cta/learn-more-copy   "Find my store"
      :sub-cta/learn-more-target [events/navigate-retail-walmart {}]})))

(def drawer-and-template-slot-ordering-v1
  [{:pdp.details/hair-info
    [:pdp.details.hair-info/model-wearing
     ;; TODO: length guide
     :pdp.details.hair-info/unit-weight
     :pdp.details.hair-info/hair-quality
     :pdp.details.hair-info/hair-origin
     :pdp.details.hair-info/hair-weft-type
     :pdp.details.hair-info/part-design
     :pdp.details.hair-info/features
     :pdp.details.hair-info/available-materials
     :pdp.details.hair-info/lace-size
     :pdp.details.hair-info/silk-size
     :pdp.details.hair-info/cap-size
     :pdp.details.hair-info/density
     :pdp.details.hair-info/tape--in-glue-information]}
   {:pdp.details/description
    [:pdp.details.description/description
     :pdp.details.description/hair-type
     :pdp.details.description/what's-included]}
   {:pdp.details/care
    [:pdp.details.care/maintenance-level
     :pdp.details.care/can-it-be-colored?]}])

(def drawer-and-template-slot-ordering-v2
  [{:pdp.details/overview
    [:pdp.details.overview/description
     :pdp.details.overview/what's-included
     :pdp.details.overview/model-wearing
     :pdp.details.overview/length-guide]}
   {:pdp.details/product-details
    [:pdp.details.product-details/unit-weight
     :pdp.details.product-details/density
     :pdp.details.product-details/hair-type
     :pdp.details.product-details/hair-weft-type
     :pdp.details.product-details/hair-quality
     :pdp.details.product-details/part-design
     :pdp.details.product-details/lace-material
     :pdp.details.product-details/lace-size
     :pdp.details.product-details/silk-size
     :pdp.details.product-details/features
     :pdp.details.product-details/cap-size
     :pdp.details.product-details/cap-sizing-guide
     :pdp.details.product-details/tape--in-glue-information]}
   {:pdp.details/care
    [:pdp.details.care/maintenance-level
     :pdp.details.care/needs-install-by-stylist?
     :pdp.details.care/can-it-be-colored?
     :pdp.details.care/view-care-guide
     :pdp.details.care/storage]}
   {:pdp.details/customize-your-wig
    [:pdp.details.customize-your-wig/in-store-services
     :pdp.details.customize-your-wig/video-tutorial]}])

(defn v1-default-template-slots<
  "Converts cellar SKU and cellar Product to template-slots"
  [current-product selected-sku model-image]
  ;;    Key                                               Title                Sub Text
  (->> [:pdp.details.description/description              nil                       (->> current-product :copy/description)
        :pdp.details.description/what's-included          "What's Included"         (->> current-product :copy/whats-included)
        :pdp.details.description/hair-type               "Hair Type"                (->> current-product :copy/hair-type)
        :pdp.details.hair-info/model-wearing             "Model Wearing"            (or (:copy/model-wearing model-image)
                                                                                        (:copy/model-wearing current-product))
        :pdp.details.hair-info/unit-weight               "Unit Weight"              (or (->> selected-sku :hair/weight)
                                                                                        (->> current-product :copy/weights))
        :pdp.details.hair-info/hair-quality              "Hair Quality"             (->> current-product :copy/quality)
        :pdp.details.hair-info/density                   "Density"                  (->> current-product :copy/density)
        :pdp.details.hair-info/hair-origin               "Hair Origin"              (->> current-product :copy/origin)
        :pdp.details.hair-info/hair-weft-type            "Hair Weft Type"           (->> current-product :copy/weft-type)
        :pdp.details.hair-info/part-design               "Part Design"              (->> current-product :copy/part-design)
        :pdp.details.hair-info/features                  "Features"                 (->> current-product :copy/features)
        :pdp.details.hair-info/available-materials       "Available Materials"      (->> current-product :copy/materials)
        :pdp.details.hair-info/lace-size                 "Lace Size"                (->> current-product :copy/lace-size)
        :pdp.details.hair-info/silk-size                 "Silk Size"                (->> current-product :copy/silk-size)
        :pdp.details.hair-info/cap-size                  "Cap Size"                 (->> current-product :copy/cap-size)
        :pdp.details.hair-info/tape--in-glue-information "Tape-in Glue Information" (->> current-product :copy/tape-in-glue)
        :pdp.details.care/maintenance-level              "Maintenance Level"        (->> current-product :copy/maintenance-level)]
       (partition 3)
       (keep (fn [[k heading content]]
               (when content
                 [k (str "<div>" (when heading (str "<h3>" heading "</h3>")) "<p>" (apply str content) "</p></div>")])))
       (into {})))

(defn v2-default-template-slots<
  "Converts cellar SKU and cellar Product to template-slots"
  [current-product selected-sku model-image]
  ;;    Key                                                    Title                                               Sub Text
  (->> [:pdp.details.overview/description                      "Description"                                      (->> current-product :copy/description)
        :pdp.details.overview/what's-included                  "What's Included"                                  (->> current-product :copy/whats-included)
        :pdp.details.overview/model-wearing                    "Model Wearing"                                    (or (:copy/model-wearing model-image)
                                                                                                                      (:copy/model-wearing current-product))
        :pdp.details.overview/length-guide                     "Length Guide"                                     nil

        :pdp.details.product-details/unit-weight               "Unit Weight"                                      (or (->> selected-sku :hair/weight)
                                                                                                                      (->> current-product :copy/weights))
        :pdp.details.product-details/density                   "Wig Density"                                      (->> current-product :copy/density)
        :pdp.details.product-details/hair-origin               "Hair Origin"                                      (->> current-product :copy/origin)
        :pdp.details.product-details/hair-weft-type            "Hair Weft Type"                                   (->> current-product :copy/weft-type)
        :pdp.details.product-details/hair-type                 "Hair Type"                                        (->> current-product :copy/hair-type)
        :pdp.details.product-details/hair-quality              "Hair Quality"                                     (->> current-product :copy/quality)
        :pdp.details.product-details/part-design               "Part Design"                                      (->> current-product :copy/part-design)
        :pdp.details.product-details/lace-marterial            "Lace Materials"                                   (->> current-product :copy/materials)
        :pdp.details.product-details/lace-size                 "Lace Size"                                        (->> current-product :copy/lace-size)
        :pdp.details.product-details/silk-size                 "Silk Size"                                        (->> current-product :copy/silk-size)
        :pdp.details.product-details/features                  "Features"                                         (->> current-product :copy/features)
        :pdp.details.product-details/cap-size                  "Cap Size"                                         (->> current-product :copy/cap-size)
        :pdp.details.product-details/tape--in-glue-information "Tape-in Glue Information"                         (->> current-product :copy/tape-in-glue)
        :pdp.details.care/maintenance-level                    "Maintenance Level"                                (->> current-product :copy/maintenance-level)
        :pdp.details.care/needs-install-by-stylist?            "Does this wig need to be installed by a stylist?" nil
        :pdp.details.care/can-it-be-colored?                   "Can this wig be colored?"                         nil
        :pdp.details.care/view-care-guide                      "View Care Guide"                                  nil
        :pdp.details.customize-your-wig/in-store-services      "In Store Services"                                nil
        :pdp.details.customize-your-wig/video-tutorial         "Video Tutorial"                                   nil]
       (partition 3)
       (keep (fn [[k heading content]]
               (when content
                 [k (str "<div>" (when heading (str "<h3>" heading "</h3>")) "<p>" (apply str content) "</p></div>")])))
       (into {})))

(defn cms-override-template-slots< [cms-template-slot-data selected-sku]
  (cms-dynamic-content/cms-and-sku->template-slot-hiccup cms-template-slot-data selected-sku))

(defn ^:private template-slot-sections< [template-slot-data length-guide-image debug-template-slots? slot-id]
  (merge
   (when-let [content (get template-slot-data slot-id)]
     {:content [:div
                (cond-> {:data-template-slot-slug (spice/kw-name slot-id)}
                  debug-template-slots? (assoc :class "debug-template-slot"))
                (if (string? content)
                  (markdown/component (markdown/md->hiccup content))
                  content)]})
   (when (and (= slot-id :pdp.details.hair-info/model-wearing)
              length-guide-image)
     {:link/content "Length Guide"
      :link/target  [events/popup-show-length-guide
                     {:length-guide-image length-guide-image
                      :location           "hair-info-tab"}]
      :link/id      "hair-info-tab-length-guide"})))

(defn ^:private template-slot-drawer< [template-slot-data length-guide-image debug-template-slots? drawer-orderings]
  (let [[drawer-id slot-ids] (first drawer-orderings)
        sections             (keep (partial template-slot-sections<
                                            template-slot-data
                                            length-guide-image
                                            debug-template-slots?)
                                   slot-ids)]
    (when (seq sections)
      {:contents {:sections sections}
       :id       drawer-id
       ;; TODO: drive drawer face copy from Contentful data?
       :face     {:copy (-> drawer-id
                            str
                            (clojure.string/split #"/")
                            last
                            (clojure.string/replace #"-" " "))}})))

(defn template-slots->accordion-slots
  [accordion-ordering template-slot-data product length-guide-image open-drawers debug-template-slots?]
  (merge
   ;; TODO drive initial-open-drawers off of Contentful Data?
   (cond
     (contains? (:catalog/department product) "stylist-exclusives")
     {:allow-all-closed?    false
      :initial-open-drawers #{:pdp.details/description}}
     (select {:hair/family #{"ready-wigs"}} [product])
     {:allow-all-closed?    true
      :initial-open-drawers #{:pdp.details/overview}}
     :else
     {:allow-all-closed?    true
      :initial-open-drawers #{:pdp.details/hair-info}})

   {:allow-multi-open?    false
    :drawers (into []
                   (keep (partial template-slot-drawer< template-slot-data length-guide-image debug-template-slots?))
                   accordion-ordering)
    :id                   :info-accordion
    :open-drawers         open-drawers}))

(defn query [data]
  (let [selections         (get-in data catalog.keypaths/detailed-product-selections)
        product            (products/current-product data)
        product-skus       (products/extract-product-skus data product)
        images-catalog     (get-in data keypaths/v2-images)
        facets             (facets/by-slug data)
        selected-sku       (get-in data catalog.keypaths/detailed-product-selected-sku)
        dyeable?           (-> selected-sku :hair/color (not= #{"s01a"})) ; Shouldn't this be a facet?
        carousel-images    (find-carousel-images product product-skus images-catalog
                                              ;;TODO These selection election keys should not be hard coded
                                                 (select-keys selections [:hair/color
                                                                          :hair/base-material])
                                                 selected-sku)

        product-options    (get-in data catalog.keypaths/detailed-product-options)
        ugc                (ugc-query product selected-sku data)
        shop?              (or (= "shop" (get-in data keypaths/store-slug))
                               (= "retail-location" (get-in data keypaths/store-experience)))
        faq                (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
                             (-> data
                                 (get-in (conj keypaths/cms-faq pdp-faq-id))
                                 (assoc :open-drawers (:accordion/open-drawers (accordion-neue/<- data :pdp-faq)))))
        selected-picker    (get-in data catalog.keypaths/detailed-product-selected-picker)
        show-good-to-know? (and (experiments/good-to-know? data)
                                (-> product :hair/family first (= "seamless-clip-ins")))]
    (merge {:title/primary     (:copy/title product)
            :ugc               ugc
            :fetching-product? (utils/requesting? data (conj request-keys/get-products
                                                             (:catalog/product-id product)))
            :adding-to-bag?    (utils/requesting? data (conj request-keys/add-to-bag
                                                             (:catalog/sku-id selected-sku)))
            :sku-quantity      (get-in data keypaths/browse-sku-quantity 1)
            :options           product-options
            :product           product
            :selections        selections
            :selected-options  (get-selected-options selections product-options)
            :selected-sku      selected-sku
            :facets            facets
            :faq-section       (when (and shop? faq)
                                 (let [{:keys [question-answers]} faq]
                                   {:faq/expanded-index (get-in data keypaths/faq-expanded-section)
                                    :list/sections      (for [{:keys [question answer]} question-answers]
                                                          {:faq/title   (:text question)
                                                           :faq/content answer})}))
            :carousel-images   carousel-images
            :selected-picker   selected-picker}
           (when show-good-to-know?
             #:pre-accordion{:primary      "Good to Know:"
                             :blocks-left  [{:primary "10 pieces (wefts) included"
                                             :icon    :svg/box-open
                                             :content ["1 x 8 inch weft" 
                                                       "1 x 7 inch weft" 
                                                       "2 x 6 inch wefts" 
                                                       "2 x 4 inch wefts" 
                                                       "4 x 1.5 inch weft"]}
                                            {:primary "Clips come attached to weft"
                                             :icon    :svg/comb}
                                            {:primary "One package creates a full look"
                                             :icon    :svg/hair-long}]
                             :blocks-right [{:primary "Can be heat-styled up to 250° F"
                                             :icon    :svg/blow-dryer
                                             :content ["Apply heat protectant before styling."]}
                                            (if dyeable?
                                              {:primary "Can be professionally colored"
                                               :icon    :svg/droplet
                                               :content ["Use deposit-only products or toners. Should not be lifted (bleached) any further."]}
                                              {:primary "Cannot be colored"
                                               :icon    :svg/droplet
                                               :content ["We do not recommend any lifting (bleaching) or coloring."]})]
                             :link-text    "Jump to FAQs"
                             :link-target  [events/control-scroll-to-selector {:selector "[data-test=faq]"}]}))))

(defn- detailed-skuer-media
  [images-catalog skuer]
  (->> skuer
       (images/for-skuer images-catalog)
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn ^:private product-carousel<-
  [images-catalog product-carousel selected-sku carousel-redesign?]
  (when carousel-redesign?
    (let [index    (:idx product-carousel)
          exhibits (->> selected-sku
                        (detailed-skuer-media images-catalog)
                        (map (fn [{:keys [alt url] :as image}]
                               {:src url
                                :alt alt
                                :type (:media/type image)})))]
      #:product-carousel
      {:selected-exhibit-idx (if (< -1 index (count exhibits))
                               index 0)
       :exhibits             exhibits})))

(defn reviews<
  [skus-db detailed-product]
  (when (and (seq detailed-product)
             (products/eligible-for-reviews? detailed-product)) ;; FIXME(corey) our product model is too anemic
    (let [;; HACK use the first variant's id as the product id for yotpo
          {:keys [legacy/variant-id]} (some->> detailed-product
                                               :selector/skus
                                               first
                                               (get skus-db))
          yotpo-data-attributes       {:data-name       (:copy/title detailed-product)
                                       :data-product-id variant-id
                                       :data-url        (routes/path-for events/navigate-product-details
                                                                         detailed-product)}]
      (merge
       (within :reviews.browser yotpo-data-attributes)
       (within :reviews.summary yotpo-data-attributes)))))

(defn zip-payment<
  [selected-sku loaded-quadpay?]
  (when (and loaded-quadpay? (seq selected-sku))
    {:zip-payments/sku-price (if (some-> selected-sku :promo.clearance/eligible first)
                               (* 0.65 (:sku/price selected-sku))
                               (:sku/price selected-sku))
     :zip-payments/loaded?   loaded-quadpay?}))

(defn price-block<
  [selected-sku]
  (when selected-sku
    (let [price (or (:product/essential-price selected-sku)
                    (:sku/price selected-sku))]
      (if (some-> selected-sku :promo.clearance/eligible first)
        {:price-block/primary-struck (mf/as-money price)
         :price-block/new-primary    (mf/as-money (* 0.65 price))
         :price-block/secondary      "each"}
        {:price-block/primary   (mf/as-money price)
         :price-block/secondary "each"}))))

(defn options-picker<
  [state facets-db options-accordion]
  (let [sku-quantity    (get-in state keypaths/browse-sku-quantity 1)
        options         (get-in state catalog.keypaths/detailed-product-options)
        selections      (get-in state catalog.keypaths/detailed-product-selections)
        selected-sku    (get-in state catalog.keypaths/detailed-product-selected-sku)
        selected-length (get-in facets-db [:hair/length :facet/options (:hair/length selections)])
        selected-color  (get-in facets-db [:hair/color :facet/options (:hair/color selections)])]
    (accordion-neue/accordion-query
     {:id                :pdp-picker
      :allow-all-closed? true
      :allow-multi-open? true
      :open-drawers      (:accordion/open-drawers options-accordion)
      :drawers           [(let [color-options (->> options :hair/color (sort-by :filter/order))]
                            {:id           "color"
                             :face         {:facet-name  "Color"
                                            :facet-slug  "color"
                                            :option-name (:option/name selected-color)
                                            :option-slug (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color))
                                            :swatch      (:option/rectangle-swatch selected-color)}
                             :open-message [events/pdp|picker-options|viewed {:facet   "color"
                                                                              :options (map :option/slug color-options)}]
                             :contents     (merge {:swatches? true
                                                   :facet     "color" 
                                                   :options   (->> color-options
                                                                   (map (fn [{:keys [option/slug option/name option/rectangle-swatch]}]
                                                                          (merge {:option-slug      (facets/hacky-fix-of-bad-slugs-on-facets slug)
                                                                                  :option-name      name
                                                                                  :rectangle-swatch rectangle-swatch
                                                                                  :selected?        (= (:option/slug selected-color) slug)}
                                                                                 (when (-> selected-color :option/slug (not= slug))
                                                                                   {:target [events/pdp|picker-options|selected
                                                                                             {:data             {:facet           "color"
                                                                                                                 :options         (map :option/slug color-options)
                                                                                                                 :selected-option slug}
                                                                                              :callback-message [events/control-product-detail-picker-option-select
                                                                                                                 {:navigation-event events/navigate-product-details
                                                                                                                  :selection        :hair/color
                                                                                                                  :value            slug}]}]})))))}
                                                  (when (-> selected-sku :hair/family first (= "seamless-clip-ins") (and (experiments/help-me-find-my-shade? state)))
                                                    {:link/content "Help me find my shade"
                                                     :link/target  [events/popup-show-shade-finder]
                                                     :link/id      "help-find-shade"}))})
                          (let [length-options (->> options
                                                    :hair/length
                                                    (sort-by :filter/order))]
                            {:id           "length"
                             :face         {:facet-name  "Length"
                                            :facet-slug  "length"
                                            :option-name (:option/name selected-length)
                                            :option-slug (:option/slug selected-length)}
                             :open-message [events/pdp|picker-options|viewed {:facet   "length"
                                                                              :options (map :option/slug length-options)}]
                             :contents     {:facet   "length"
                                            :options (->> length-options
                                                          (map (fn [{:keys [option/name option/slug]}]
                                                                 (merge {:copy        name
                                                                         :option-slug slug
                                                                         :selected?   (= (:option/slug selected-length) slug)}
                                                                        (when (-> selected-length :option/slug (not= slug))
                                                                          {:target [events/pdp|picker-options|selected
                                                                                    {:data             {:facet           "length"
                                                                                                        :options         (map :option/slug length-options)
                                                                                                        :selected-option slug}
                                                                                     :callback-message [events/control-product-detail-picker-option-select
                                                                                                        {:navigation-event events/navigate-product-details
                                                                                                         :selection        :hair/length
                                                                                                         :value            slug}]}]})))))}})
                          (let [qty-options (->> (range)
                                                 (take 10)
                                                 (map inc))]
                            {:id           "quantity"
                             :face         {:facet-name  "Qty"
                                            :facet-slug  "quantity"
                                            :option-name sku-quantity
                                            :option-slug sku-quantity}
                             :open-message [events/pdp|picker-options|viewed {:facet   "quantity"
                                                                              :options (map str qty-options)}]
                             :contents     {:facet   "quantity"
                                            :options (->> qty-options
                                                          (map (fn [qty]
                                                                 (merge
                                                                  {:copy        (str qty)
                                                                   :option-slug (str qty)
                                                                   :selected?   (= sku-quantity qty)}
                                                                  (when (not (= sku-quantity qty))
                                                                    {:target [events/pdp|picker-options|selected
                                                                              {:data             {:facet           "quantity"
                                                                                                  :options         (->> (range) (take 10) (map inc) (map str))
                                                                                                  :selected-option (str qty)}
                                                                               :callback-message [events/control-product-detail-picker-option-quantity-select
                                                                                                  {:value qty}]}]})))))}})]})))

(defn information<
  [state images-db info-accordion detailed-product selected-sku]
  (let [length-guide-image    (->> (images/for-skuer images-db detailed-product)
                                   (select {:use-case #{"length-guide"}})
                                  first)
        debug-template-slots? (experiments/debug-template-slots? state)

        ;; Selections through model-image is so that we can display model wearing
        ;; in the information section for the old carousel
        selections      (get-in state catalog.keypaths/detailed-product-selections)
        product-skus    (products/extract-product-skus state detailed-product)
        carousel-images (find-carousel-images detailed-product product-skus images-db
                                              ;;TODO These selection election keys should not be hard coded
                                              (select-keys selections [:hair/color
                                                                       :hair/base-material])
                                              selected-sku)
        model-image     (first (filter :copy/model-wearing carousel-images))

        use-cms-only?                (experiments/pdp-template-slots-from-cms-only? state)
        use-v2-drawers?              (experiments/pdp-template-slots? state)
        accordion-ordering           (if use-v2-drawers?
                                       drawer-and-template-slot-ordering-v2
                                       drawer-and-template-slot-ordering-v1)
        accordion-template-slot-data (merge (when (and (not use-v2-drawers?)
                                                       (not use-cms-only?))
                                              (v1-default-template-slots< detailed-product
                                                                          selected-sku
                                                                          model-image))
                                            (when (and use-v2-drawers?
                                                       (not use-cms-only?))
                                              (v2-default-template-slots< detailed-product
                                                                          selected-sku
                                                                          model-image))
                                            (when use-v2-drawers?
                                              (cms-override-template-slots< (get-in state keypaths/cms-template-slots)
                                                                            selected-sku)))]
    (accordion-neue/accordion-query
     (template-slots->accordion-slots accordion-ordering
                                      accordion-template-slot-data
                                      detailed-product
                                      length-guide-image
                                      (:accordion/open-drawers info-accordion)
                                      debug-template-slots?))))

(defn ^:export page
  [state opts]
  (let [;; Databases
        facets-db          (facets/by-slug state)
        images-db          (get-in state keypaths/v2-images)
        skus-db            (get-in state keypaths/v2-skus)
        product-carousel   (carousel-neue/<- state :product-carousel)
        options-accordion  (accordion-neue/<- state :pdp-picker)
        info-accordion     (accordion-neue/<- state :info-accordion)
        ;; external loads
        loaded-quadpay?    (get-in state keypaths/loaded-quadpay)
        ;; Focus
        detailed-product   (products/current-product state)
        selected-sku       (get-in state catalog.keypaths/detailed-product-selected-sku) 
        ;; Flags
        carousel-redesign? (and (experiments/carousel-redesign? state)
                                (or (select ?wig [detailed-product])
                                    (select {:hair/family #{"seamless-clip-ins"}} [detailed-product])))]
    (c/build (if detailed-product template loading-template)
             (merge (query state)
                    (options-picker< state facets-db options-accordion)
                    {:add-to-cart (add-to-cart-query state)}
                    (product-carousel<- images-db product-carousel selected-sku carousel-redesign?)
                    (price-block< selected-sku)
                    (zip-payment< selected-sku loaded-quadpay?)
                    (information< state
                                  images-db
                                  info-accordion
                                  detailed-product
                                  selected-sku)
                    (reviews< skus-db detailed-product)
                    opts))))

(defn url-points-to-invalid-sku? [selected-sku query-params]
  (boolean
   (and (:catalog/sku-id selected-sku)
        (not= (:catalog/sku-id selected-sku)
              (:SKU query-params)))))

#?(:cljs
   (defn fetch-product-details [app-state product-id]
     (api/get-products (get-in app-state keypaths/api-cache)
                       {:catalog/product-id product-id}
                       (fn [response]
                         (messages/handle-message events/api-success-v3-products-for-details response)
                         (when-let [selected-sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)]
                           (messages/handle-message events/viewed-sku {:sku selected-sku}))))))

(defn generate-product-options
  [product-id app-state]
  (let [product      (products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (products/extract-product-skus app-state product)
        images       (get-in app-state keypaths/v2-images)]
    (sku-selector/product-options facets product product-skus images)))

(defn ^:private assoc-selections
  "Selections are ultimately a function of three inputs:
   1. options of the cheapest sku
   2. prior selections on another product, but validated for this product
   3. options of the sku from the URI

   They are merged together each (possibly partially) overriding the
   previous selection map of options.

   NB: User clicks for selections are indirectly used by changing the URI."
  [app-state sku]
  (let [product-id   (get-in app-state catalog.keypaths/detailed-product-id)
        product      (products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (products/extract-product-skus app-state product)
        images       (get-in app-state keypaths/v2-images)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selections
                  (merge
                   (default-selections facets product product-skus images)
                   (let [{:catalog/keys [department]
                          :product?essential-service/keys [electives]}
                         (api.products/product<- app-state product)]
                     (when (= #{"service"} department)
                       electives))
                   (get-in app-state catalog.keypaths/detailed-product-selections)
                   (reduce-kv #(assoc %1 %2 (first %3))
                              {}
                              (select-keys sku (:selector/electives product))))))))

(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ _ {:keys [selection value]} app-state]
  ;; HACK [#179260091]
  (let [switching-to-short-hd-lace? (and (= [selection value] [:hair/base-material "hd-lace"])
                                         (-> app-state
                                             (get-in catalog.keypaths/detailed-product-selections)
                                             :hair/length
                                             spice/parse-int
                                             (< 16)))
        selected-sku                (->> (if switching-to-short-hd-lace?
                                           {selection #{value} :hair/length #{"16"}}
                                           {selection #{value}})
                                         (determine-sku-from-selections app-state))
        options                     (generate-product-options (get-in app-state catalog.keypaths/detailed-product-id)
                                                              app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
        (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
        (update-in catalog.keypaths/detailed-product-selections merge (if switching-to-short-hd-lace?
                                                                        {selection value :hair/length "16"}
                                                                        {selection value}))
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod effects/perform-effects events/control-product-detail-picker-option-select
  [_ _ {:keys [navigation-event]} _ app-state]
  (let [sku-id-for-selection (-> app-state
                                 (get-in catalog.keypaths/detailed-product-selected-sku)
                                 :catalog/sku-id)
        params-with-sku-id   (cond-> (select-keys (products/current-product app-state)
                                                  [:catalog/product-id :page/slug])
                               (some? sku-id-for-selection)
                               (assoc :query-params {:SKU sku-id-for-selection}))]
    (effects/redirect navigation-event params-with-sku-id :sku-option-select)
    #?(:cljs (scroll/enable-body-scrolling))))

(defmethod transitions/transition-state events/control-product-detail-picker-open
  [_ _ {:keys [facet-slug length-index]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-selected-picker facet-slug)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? true)
      (assoc-in catalog.keypaths/detailed-product-lengths-index length-index)))

(defmethod transitions/transition-state events/control-product-detail-picker-option-quantity-select
  [_ _ {:keys [value]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-sku-quantity value)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)))

(defmethod transitions/transition-state events/control-product-detail-picker-close
  [_ _ _ app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
      (assoc-in catalog.keypaths/detailed-product-lengths-index nil)
      (assoc-in catalog.keypaths/detailed-product-selected-picker nil)))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-open
     [_ _ _ _ _]
     (scroll/disable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-close
     [_ _ _ _ _]
     (scroll/enable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args prev-app-state app-state]
     (let [[prev-event prev-args] (get-in prev-app-state keypaths/navigation-message)
           product-id (:catalog/product-id args)
           product (get-in app-state (conj keypaths/v2-products product-id))
           just-arrived? (or (not= events/navigate-product-details prev-event)
                             (not= (:catalog/product-id args)
                                   (:catalog/product-id prev-args))
                             (= :first-nav (:navigate/caused-by args)))]
       (when (experiments/pdp-template-slots? app-state)
         (effects/fetch-cms2 app-state [:templateSlot]))
       (when (nil? product)
         (fetch-product-details app-state product-id))
       (when just-arrived?
         (messages/handle-message events/initialize-product-details (assoc args :origin-nav-event event)))
       (messages/handle-message events/pdp|viewed args))))

#?(:cljs
   (defmethod trackings/perform-track events/pdp|viewed
     [_ _ {:keys [catalog/product-id query-params]} app-state] 
     (when-let [sku (or (->> (:SKU query-params)
                             (conj keypaths/v2-skus)
                             (get-in app-state))
                        (get-in app-state catalog.keypaths/detailed-product-selected-sku)
                        (->> product-id
                             (conj keypaths/v2-products)
                             (get-in app-state)
                             (products/extract-product-skus app-state)
                             first))]
       (google-analytics/track-view-item sku))
     (when (-> product-id
               ((get-in app-state keypaths/v2-products))
               accessors.products/wig-product?)
       (facebook-analytics/track-event "wig_content_fired"))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-v3-products-for-details
     [_ _ _ _ app-state]
     (messages/handle-message events/initialize-product-details (get-in app-state keypaths/navigation-args))))

(defmethod transitions/transition-state events/initialize-product-details
  [_ _ {:as args :keys [catalog/product-id query-params]} app-state]
  (let [product-options   (generate-product-options product-id app-state)
        product           (products/product-by-id app-state product-id)
        product-skus      (products/extract-product-skus app-state product)
        sku               (or (->> (:SKU query-params)
                                   (conj keypaths/v2-skus)
                                   (get-in app-state))
                              (first product-skus))
        availability      (catalog.products/index-by-selectors
                           [:hair/color :hair/length]
                           product-skus)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in keypaths/browse-sku-quantity 1)
        (assoc-in catalog.keypaths/detailed-product-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-product-picker-visible? nil)
        (assoc-in catalog.keypaths/detailed-product-availability availability)
        (assoc-selections sku)
        (assoc-in catalog.keypaths/detailed-product-options product-options))))

#?(:cljs
   (defmethod effects/perform-effects events/initialize-product-details
     [_ _ {:keys [catalog/product-id page/slug query-params origin-nav-event]} _ app-state]
     (let [selected-sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)
           shop?        (= :shop (sites/determine-site app-state))]
       (if (url-points-to-invalid-sku? selected-sku query-params)
         (effects/redirect origin-nav-event
                           (merge
                            {:catalog/product-id product-id
                             :page/slug          slug}
                            (when selected-sku
                              {:query-params {:SKU (:catalog/sku-id selected-sku)}})))
         (let [product (get-in app-state (conj keypaths/v2-products product-id))]
           (when-not (auth/permitted-product? app-state product)
             (effects/redirect events/navigate-home))
           (publish events/reviews|reset)
           (seo/set-tags app-state)
           (when-let [album-keyword (storefront.ugc/product->album-keyword shop? product)]
             (effects/fetch-cms-keypath app-state [:ugc-collection album-keyword]))
           (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
             (effects/fetch-cms-keypath app-state [:faq pdp-faq-id])))))))


(defmethod effects/perform-effects events/control-add-sku-to-bag
  [_ _ {:keys [sku quantity]} _ _]
  (messages/handle-message events/add-sku-to-bag
                           {:sku           sku
                            :stay-on-page? false
                            :quantity      quantity}))

;; TODO(corey) Move this to cart
(defmethod effects/perform-effects events/add-sku-to-bag
  [_ _ {:keys [sku quantity stay-on-page?]} _ state]
  #?(:cljs
     (let [nav-event          (get-in state keypaths/navigation-event)]
       (api/add-sku-to-bag
        (get-in state keypaths/session-id)
        {:sku                sku
         :quantity           quantity
         :stylist-id         (get-in state keypaths/store-stylist-id)
         :token              (get-in state keypaths/order-token)
         :number             (get-in state keypaths/order-number)
         :user-id            (get-in state keypaths/user-id)
         :user-token         (get-in state keypaths/user-token)
         :heat-feature-flags (keys (filter second (get-in state keypaths/features)))}
        #(do
           (messages/handle-message events/api-success-add-sku-to-bag
                                    {:order    %
                                     :quantity quantity
                                     :sku      sku})
           (when (not (or (= events/navigate-cart nav-event) stay-on-page?))
             (history/enqueue-navigate events/navigate-cart)))))))

;; FIXME(corey) This seems broken and pretty far from the original intent
(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ _ {:keys [quantity sku]} app-state]
  (assoc-in app-state keypaths/browse-sku-quantity 1))

#?(:cljs
   (defmethod trackings/perform-track events/pdp|picker-options|viewed
     [_ _ data _]
     (stringer/track-event "pdp.picker-options/viewed" data)))

#?(:cljs
   (defmethod effects/perform-effects events/pdp|picker-options|selected
     [_ _ {:keys [data callback-message]} _ _]
     (apply messages/handle-message callback-message)))

#?(:cljs
   (defmethod trackings/perform-track events/pdp|picker-options|selected
     [_ _ {:keys [data]} _]
     (stringer/track-event "pdp.picker-options/selected" data)))

