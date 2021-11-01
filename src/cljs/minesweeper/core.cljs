(ns minesweeper.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [minesweeper.game :as game]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))


(rf/reg-event-db
 :minesweeper/new-game
 (fn [db [_]]
   (assoc db :minesweeper/game (game/make-game :trivial))))

(rf/reg-event-db
 :minesweeper/reveal
 (fn [db [_ cell]]
   (update db :minesweeper/game game/reveal cell)))

(rf/reg-event-db
 :minesweeper/toggle-flag
 (fn [db [_ cell]]
   (update db :minesweeper/game game/toggle-flag cell)))

(rf/reg-sub
 :minesweeper/game
 (fn [db _]
   (-> db :minesweeper/game)))

(rf/reg-sub
 :minesweeper/rows
 (fn [db _]
   (-> db :minesweeper/game :rows)))

(rf/reg-sub
 :minesweeper/cols
 (fn [db _]
   (-> db :minesweeper/game :cols)))

(rf/reg-sub
 :minesweeper/cell
 (fn [db [_ cell]]
   (-> db
       :minesweeper/game
       :board
       (get cell))))


(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "minesweeper"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]]))

(defn minesweeper-cell [cell]
  (let [state @(rf/subscribe [:minesweeper/cell cell])]
    [:div.board-cell
     {:style {:width "20px" :height "20px" :float :left :border ["solid 1px"] :text-align :center}}
      (case state
        :flagged
        [:button {:style {:width "100%" :height "100%" :padding "0px" :margin "0px"}
                  :on-context-menu #(do
                                      (rf/dispatch [:minesweeper/toggle-flag cell])
                                      (.preventDefault %))
                  :dangerouslySetInnerHTML {:__html "&#x1f6a9;"}}]

        :hidden
        [:button {:style {:width "100%" :height "100%" :padding "0px" :margin "0px"}
                  :on-click #(rf/dispatch [:minesweeper/reveal cell])
                  :on-context-menu #(do
                                      (rf/dispatch [:minesweeper/toggle-flag cell])
                                      (.preventDefault %))}]

        :mine
        [:span {:dangerouslySetInnerHTML {:__html "&#x1f4a3;"}}]

        0
        ""
        (str state))]))

(defn minesweeper []
  [:div.board
   (let [rows @(rf/subscribe [:minesweeper/rows])
         cols @(rf/subscribe [:minesweeper/cols])]
     (for [r (range rows)]
       [:div.board-row
        {:style {:clear :both}}
        (for [c (range cols)]
          (let [cell {:row r :col c}]
            ^{:key cell} [minesweeper-cell cell]))]))])

(defn home-page []
  [:div
   [navbar]
   [minesweeper]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch [:minesweeper/new-game])
  (mount-components))
