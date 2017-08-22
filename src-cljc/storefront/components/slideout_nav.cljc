(ns storefront.components.slideout-nav
  (:require [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.api :as api]
                       [storefront.frontend-effects :as frontend-effects]
                       [storefront.component :as component]])
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.auth :as auth]
            [storefront.utils.query :as query]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.category-filters :as category-filters]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.marquee :as marquee]
            [clojure.string :as str]
            [storefront.platform.component-utils :as utils]
            [storefront.assets :as assets]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.ui :as ui]
            [datascript.core :as d]
            [clojure.set :as set]
            [storefront.components.svg :as svg]
            [storefront.utils.maps :as maps]))

(def blog-url "https://blog.mayvenn.com")

(defn promo-bar [promo-data]
  (component/build promotion-banner/component promo-data nil))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "70px"}}
    [:div.relative.rotate-45.p2 {:style     {:height "70px"}
                                 :data-test "close-slideout"
                                 :on-click  #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "25px" :height "50px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "50px" :height "25px"}}]]]))

(def back-caret
  (component/html
   (svg/dropdown-arrow {:class  "stroke-gray"
                        :width  "12px"
                        :height "10px"
                        :style  {:transform "rotate(90deg)"}})))

(def forward-caret
  (component/html
   (svg/dropdown-arrow {:class  "stroke-black"
                        :width  "23px"
                        :height "20px"
                        :style  {:transform "rotate(-90deg)"}})))

(defn logo [data-test-value height]
  [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
   (assoc (utils/route-to events/navigate-home)
          :style {:height height}
          :title "Mayvenn"
          :item-prop "logo"
          :data-test data-test-value
          :content (str "https:" (assets/path "/images/header_logo.svg")))])

(def burger-header
  (component/html
   [:div.bg-white
    menu-x
    [:div.center.col-12.p3 (logo "header-logo" "40px")]]))

(defn ^:private marquee-col [content]
  [:div.flex-auto
   {:style {:flex-basis 0}}
   content])

(defn marquee-row [left-content right-content]
  [:div.flex.my3
   (marquee-col left-content)
   [:div.pr3]
   (marquee-col right-content)])

(def social-link :a.inherit-color.h6.underline)

(def ^:private gallery-link
  (component/html
   [social-link
    (utils/route-to events/navigate-gallery)
    "View gallery"]))

(defn ^:private instagram-link [instagram-account]
  [social-link
   {:href (marquee/instagram-url instagram-account)}
   "Follow"])

(defn ^:private styleseat-link [styleseat-account]
  [social-link
   {:href (marquee/styleseat-url styleseat-account)}
   "Book"])

(defn store-actions [{:keys [store-nickname] :as store}]
  [:div
   [:div.h7.medium "Welcome to " store-nickname "'s store"]
   [:div.dark-gray
    (interpose " | "
               (marquee/actions store gallery-link instagram-link styleseat-link))]])

(defn portrait [signed-in {:keys [portrait]}]
  (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
    ::marquee/show-what-we-have [:div.left.self-center.pr2 (marquee/stylist-portrait portrait)]
    ::marquee/ask-for-portrait  [:div.left.self-center.pr2 marquee/add-portrait-cta]
    ::marquee/show-nothing      nil))

(defn store-info-marquee [signed-in store]
  (when (-> signed-in ::auth/to (= :marketplace))
    [:div.my3.flex
     (portrait signed-in store)
     (store-actions store)]))

(defn account-info-marquee [signed-in {:keys [email store-credit]}]
  (when (-> signed-in ::auth/at-all)
    [:div.my3
     [:div.h7.medium "Signed in with:"]
     [:a.teal.h5
      (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                        events/navigate-stylist-account-profile
                        events/navigate-account-manage))
      email]
     (when (pos? store-credit)
       [:p.teal.h5 "You have store credit: " (as-money store-credit)])]))

(def stylist-actions
  (component/html
   [:div
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-account-profile)
                             :data-test "account-settings")
                      "Manage account")
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-share-your-store)
                             :data-test "share-your-store")
                      "Share your store"))
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-dashboard-commissions)
                             :data-test "dashboard")
                      "Dashboard")
     (ui/ghost-button stylists/community-url
                      "Community"))]))

(def user-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                            :data-test "account-settings")
                     "Manage account")
    (ui/ghost-button (utils/route-to events/navigate-account-referrals)
                     "Refer a friend"))))

(def guest-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-sign-in)
                            :data-test "sign-in")
                     "Sign in")
    [:div.h6.col-12.center.dark-gray
     [:div "No account?"]
     [:a.inherit-color.underline
      (assoc (utils/route-to events/navigate-sign-up)
             :data-test "sign-up")
      "Sign up now, get offers!"]])))

(defn actions-marquee [signed-in]
  (case (-> signed-in ::auth/as)
    :stylist stylist-actions
    :user    user-actions
    :guest   guest-actions))

(defn minor-menu-row [& content]
  [:div.border-bottom.border-gray
   {:style {:padding "3px 0 2px"}}
   (into [:a.block.py1.h5.inherit-color.flex.items-center] content)])

(defn major-menu-row [& content]
  [:div.h4.border-bottom.border-gray.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defn old-shopping-area [signed-in named-searches]
  [[:li {:key "old-shopping-area"}
    (minor-menu-row (utils/route-to events/navigate-shop-by-look)
                    "Shop looks")]
   (for [{:keys [title items]} (cond-> [{:title "Shop hair"
                                         :items (filter named-searches/is-extension? named-searches)}
                                        {:title "Shop closures & frontals"
                                         :items (filter named-searches/is-closure-or-frontal? named-searches)}]
                                 (-> signed-in ::auth/as (= :stylist))
                                 (conj {:title "Stylist exclusives"
                                        :items (filter named-searches/is-stylist-product? named-searches)}))]
     [:li {:key title}
      (minor-menu-row title)
      [:ul.list-reset.ml6
       (for [{:keys [name slug]} items]
         [:li {:key slug}
          (minor-menu-row (assoc (utils/route-to events/navigate-named-search {:named-search-slug slug})
                                 :data-test (str "menu-" slug))
                          (when (named-searches/new-named-search? slug) [:span.teal.mr1 "NEW"])
                          (str/capitalize name))])]])])

(defn shopping-area [signed-in]
  [:div
   [:li (major-menu-row (utils/route-to events/navigate-shop-by-look) [:span.medium "Shop Looks"])]
   [:li (major-menu-row (utils/fake-href events/menu-traverse-descend {:slug "bundles" :id "11"})
                        [:span.medium.flex-auto "Shop Bundles"]
                        forward-caret)]
   [:li (major-menu-row (utils/fake-href events/menu-traverse-descend {:slug "closures-and-frontals" :id "12"})
                        [:span.medium.flex-auto "Shop Closures & Frontals"]
                        forward-caret)]
   (when (-> signed-in ::auth/as (= :stylist))
     [:li (major-menu-row (utils/route-to events/navigate-named-search {:named-search-slug "stylist-products"})
                          [:span.medium.flex-auto "Shop Stylist Exclusives"])])])

(defn menu-area [signed-in new-taxon-launch? {:keys [named-searches]}]
  [:ul.list-reset.mb3
   (if new-taxon-launch?
     (shopping-area signed-in)
     (old-shopping-area signed-in named-searches))
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                               :data-test "content-guarantee")
                        "Our guarantee")]
   [:li (minor-menu-row {:href blog-url}
                        "Real Beautiful blog")]
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-about-us)
                               :data-test "content-about-us")
                        "About us")]
   [:li (minor-menu-row {:href "https://jobs.mayvenn.com"}
                        "Careers")]
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-help)
                               :data-test "content-help")
                        "Contact us")]])

(def sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(def slug->jump-out
  {"360-frontals" {:id   "10"
                   :name "360 Frontals"
                   :slug "360-frontals"}
   "closures"     {:id   "0"
                   :name "Closures"
                   :slug "closures"}
   "frontals"     {:id   "1"
                   :name "Frontals"
                   :slug "frontals"}})

(defn taxonomy-component
  [{:keys [root-name facets criteria promo-data]} owner opts]
  (let [[selected-steps unselected-steps] (split-with :selected? facets)
        current-step                      (last selected-steps)
        up-step                           (last (drop-last selected-steps))
        down-step                         (first unselected-steps)
        up-step-option                    (first (filterv :selected? (:options up-step)))]
    (component/create
     [:div
      [:div.top-0.sticky.z4
       (promo-bar promo-data)
       burger-header]
      [:a.gray.block.py1.px3.h6
       (if up-step
         (utils/fake-href events/menu-traverse-ascend {:up-step up-step})
         (utils/fake-href events/menu-traverse-root))
       [:span.mr1 back-caret] "Back"]
      [:div.px6
       (major-menu-row
        (let [criteria-labels (str/join " " (map :label (vals criteria)))]
          [:div.h2.flex-auto.center
           "Shop " criteria-labels " " root-name]))
       [:ul.list-reset
        (when-let [jump-out (get slug->jump-out (:slug up-step-option))]
          [:li
           (major-menu-row
            (utils/fake-href events/navigate-category (select-keys jump-out [:id :slug]))
            [:span.teal "All " (:name jump-out)])])
        (for [option (:options current-step)]
          (let [selected-options (->> selected-steps
                                      (map :options)
                                      (map #(filterv :selected? %))
                                      (remove empty?))
                valid-branch?    (or (empty? selected-options)
                                     (->> selected-options
                                          (map (partial map :sku-set-ids))
                                          (map (partial reduce set/union))
                                          (reduce set/intersection (:sku-set-ids option))
                                          seq))]
            (when valid-branch?
              (if down-step
                [:li {:key (:slug option)}
                 (major-menu-row
                  (utils/fake-href events/menu-traverse-descend
                                   {:down-step       down-step
                                    :current-step    current-step
                                    :selected-option option})
                  [:span.flex-auto (:label option)] forward-caret)]
                [:li {:key (:slug option)}
                 (major-menu-row
                  (utils/fake-href events/menu-traverse-out {:criteria (assoc criteria (:slug current-step) #{(:slug option)})})
                  [:span.flex-auto (:label option)])]))))]]])))

(defn slideout-component
  [{:keys [user store promo-data shopping signed-in new-taxon-launch?] :as data}
   owner
   opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4.border-gray
     (promo-bar promo-data)
     burger-header]
    [:div.px6.border-bottom.border-top.border-gray
     (store-info-marquee signed-in store)
     (account-info-marquee signed-in user)
     [:div.my3.dark-gray
      (actions-marquee signed-in)]]
    [:div.px6
     (menu-area signed-in new-taxon-launch? shopping)]
    (when (-> signed-in ::auth/at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn component
  [{:keys [on-taxon? new-taxon-launch? category-menu] :as data} owner opts]
  (component/create
   [:div
    (if (and on-taxon? new-taxon-launch?)
      (component/build taxonomy-component
                       (merge
                        category-menu
                        (select-keys data [:promo-data]))
                       opts)
      (component/build slideout-component data opts))]))

(defn basic-query [data]
  {:signed-in         (auth/signed-in data)
   :new-taxon-launch? (experiments/new-taxon-launch? data)
   :on-taxon?         (get-in data keypaths/current-traverse-nav-id)
   :user              {:email (get-in data keypaths/user-email)}
   :store             (marquee/query data)
   :shopping          {:named-searches (named-searches/current-named-searches data)}})

(defn menu-traverse-query [data]
  (let [{root-name :name}         (categories/current-traverse-nav data)
        {:keys [facets criteria] :as filters} (get-in data keypaths/category-filters-for-nav)]
    {:root-name root-name
     :facets    facets
     :criteria  criteria}))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:promo-data] (promotion-banner/query data))
      (assoc-in [:category-menu] (menu-traverse-query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))

(defmethod transitions/transition-state events/menu-traverse-root
  [_ _ {:keys [id]} app-state]
  (-> app-state
      (assoc-in  keypaths/category-filters-for-nav {})
      (update-in keypaths/current-traverse-nav dissoc :id)))

#?(:cljs (defmethod effects/perform-effects events/menu-traverse-root
           [_ _ _ _ app-state]
           (api/fetch-facets (get-in app-state keypaths/api-cache))))

(defn ascend [filters {facet-slug :slug :as up-step}]
  (let [option-slug (-> (:criteria filters)
                        (get facet-slug)
                        first)]
    (-> filters
        (category-filters/undo-criterion)
        (category-filters/step up-step))))

(defn descend [filters current-step selected-option down-step]
  (-> filters
      (category-filters/replace-criterion (:slug current-step)
                                          (:slug selected-option))
      (category-filters/step down-step)))

(defmethod transitions/transition-state events/menu-traverse-descend
  [_ _ {:keys [id current-step selected-option down-step]} app-state]
  (if id
    (assoc-in app-state keypaths/current-traverse-nav-id id)
    (update-in app-state keypaths/category-filters-for-nav
               descend current-step selected-option down-step)))

#?(:cljs
   (defmethod effects/perform-effects events/menu-traverse-descend
     [_ _ {:keys [id slug] :as args} _ app-state]
     (when id
       (let [category (categories/current-traverse-nav app-state)]
         (api/search-sku-sets (:criteria category)
                              #(messages/handle-message events/api-success-sku-sets-for-nav
                                                        (assoc %
                                                               :category-id (:id category)
                                                               :criteria {})))))))

(defmethod transitions/transition-state events/menu-traverse-ascend
  [_ _ {:keys [up-step criteria]} app-state]
  (update-in app-state
             keypaths/category-filters-for-nav
             ascend up-step))

(defmethod transitions/transition-state events/api-success-sku-sets-for-nav
  [_ event response app-state]
  (let [filters     (categories/make-category-filters app-state response)
        remove-skus (partial map #(dissoc % :skus))]
    (assoc-in app-state keypaths/category-filters-for-nav
              (-> filters
                  (update :initial-sku-sets remove-skus)
                  (update :filtered-sku-sets remove-skus)
                  (category-filters/open (-> filters
                                             :facets
                                             first
                                             :slug))))))

(defn ->clauses [m] (mapv (fn [[k v]] ['?s k v]) m))

#?(:cljs
   (defmethod effects/perform-effects events/menu-traverse-out
     [_ event {:keys [criteria]} _ app-state]
     (let [sku-sets-db   (-> (d/empty-db)
                             (d/db-with (->> (get-in app-state keypaths/sku-sets)
                                             vals
                                             (map #(merge (maps/map-values first (:criteria %)) %))
                                             (mapv #(dissoc % :criteria)))))

           initial-query (when (seq criteria)
                           (concat [:find '(pull ?s [*])
                                    :where]
                                   (->clauses (maps/map-values first criteria))))

           sku-set       (when (seq initial-query)
                           (->> (d/q initial-query sku-sets-db)
                                ffirst))]
       (frontend-effects/redirect events/navigate-product-details (select-keys sku-set [:id :slug])))))

(defmethod effects/perform-effects events/control-menu-expand-hamburger
  [_ _ _ _ _]
  (messages/handle-message events/menu-traverse-root))
