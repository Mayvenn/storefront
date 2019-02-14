(ns adventure.albums
  (:require [storefront.events :as events]
            [adventure.progress :as progress]))

;; This data is very tied to a presentation schema
;; TODO consider a coherent schema for albums and convert to presentation
;; in a particular screens query.

(def by-keyword
  {:shop-by-look-straight
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/ffe3011a-1cae-494a-a806-eac94f618374/-/format/auto/bg.png"}

   :shop-by-look-loose-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/ffe3011a-1cae-494a-a806-eac94f618374/-/format/auto/bg.png"}

   :shop-by-look-body-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/ffe3011a-1cae-494a-a806-eac94f618374/-/format/auto/bg.png"}

   :shop-by-look-deep-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-look
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/ffe3011a-1cae-494a-a806-eac94f618374/-/format/auto/bg.png"}

   :bundle-sets
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}


   :bundle-sets-straight
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-yaki-straight
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-kinky-straight
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-loose-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-body-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-deep-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-water-wave
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   :bundle-sets-curly
   {:header-data  {:title                   "The New You"
                   :shopping-bag?           true
                   :progress                progress/select-new-look-shop-by-bundlesets
                   :back-navigation-message [events/navigate-adventure-how-shop-hair]}
    :prompt       "Select your new look"
    :mini-prompt  ["We have an amazing selection for you"
                   [:br]
                   "to choose from."]
    :prompt-image "//ucarecdn.com/9806a920-9c5b-4a98-a9dc-21b02c381593/-/format/auto/bg.png"}

   })
