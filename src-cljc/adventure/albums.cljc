(ns adventure.albums
  (:require [storefront.events :as events]
            [adventure.progress :as progress]))

;; This data is very tied to a presentation schema
;; TODO consider a coherent schema for albums and convert to presentation
;; in a particular screens query.

(def by-keyword
  {:shop-by-look-straight
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Choose your Straight look"
    :mini-prompt  ["We have an amazing selection of inspo" [:br] "to choose from."]
    :prompt-image "//ucarecdn.com/5cea0be7-63a3-4518-919e-a16caa500a44/-/format/auto/bg.png"}

   :shop-by-look-loose-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Choose your Loose Wave look"
    :mini-prompt  ["We have an amazing selection of inspo" [:br] "to choose from."]
    :prompt-image "//ucarecdn.com/b9dd2e0b-27a0-4857-ae90-00b6b90923f0/-/format/auto/bg.png"}

   :shop-by-look-body-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Choose your Body Wave look"
    :mini-prompt  ["We have an amazing selection of inspo" [:br] "to choose from."]
    :prompt-image "//ucarecdn.com/96967158-921d-4ca7-868c-2b3ef2265a6b/-/format/auto/bg.png"}

   :shop-by-look-deep-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Choose your Deep Wave look"
    :mini-prompt  ["We have an amazing selection of inspo" [:br] "to choose from."]
    :prompt-image "//ucarecdn.com/7a7a7475-dfad-471f-bf5e-1954e9f8c8ce/-/format/auto/bg.png"}

   :bundle-sets-straight
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Straight bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/1762e2b7-ff4d-4464-8bf8-e179e9aaa6f6/-/format/auto/bg.png"}

   :bundle-sets-yaki-straight
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Yaki Straight bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/cc74be53-dbd8-464a-8415-b15ad011af83/-/format/auto/bg.png"}

   :bundle-sets-kinky-straight
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Kinky Straight bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/00821723-4ba6-4489-a6ad-f57a15f0826c/-/format/auto/bg.png"}

   :bundle-sets-loose-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Loose Wave bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/78dc9a44-6cb7-40ff-9a4e-b33d2f45ef47/-/format/auto/bg.png"}

   :bundle-sets-body-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Body Wave bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/939c620c-9dae-49c5-9294-827966d80fd3/-/format/auto/bg.png"}

   :bundle-sets-deep-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Deep Wave bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/eb490567-6066-44d4-999e-7200146416ea/-/format/auto/bg.png"}

   :bundle-sets-water-wave
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Water Wave bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/6ca376e6-8f05-4884-b6ca-3bc55200f61d/-/format/auto/bg.png"}

   :bundle-sets-curly
   {:header-data  {:title                   "The New You"
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Our fave Curly bundle sets"
    :mini-prompt  ["The best combinations, period."]
    :prompt-image "//ucarecdn.com/42f3d339-8c69-4325-8581-ffb43858b116/-/format/auto/bg.png"}})
