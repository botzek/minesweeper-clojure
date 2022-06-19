(ns minesweeper.game
  (:require
    [clojure.set :as set]))

(declare mine-generator-default)

(defn make-game
  ([{:keys [rows cols mines]} mine-generator]
   (make-game rows cols mines mine-generator))
  ([rows cols mine-count mine-generator]
   (let [cells (for [r (range rows)
                     c (range cols)]
                 {:row r :col c})
         board (zipmap cells (repeat :hidden))]
     {:mine-count mine-count
      :mine-generator (or mine-generator mine-generator-default)
      :rows rows
      :cols cols
      :board board
      :status :playing})))

;; Pre-Set Difficulties
(def difficulties
  (array-map
   :trivial {:name "Trivial" :rows 9 :cols 9 :mines 5}
   :beginner {:name "Beginner" :rows 9 :cols 9 :mines 10}
   :intermediate {:name "Intermediate" :rows 16 :cols 16 :mines 40}
   :expert {:name "Expert" :rows 16 :cols 30 :mines 99}))

(defn- cell-adjacent?
  "Returns true when the two cells are adjacent to eachother."
  [{:keys [row col] :as cell} other]
  (let [min-row (- row 1)
        max-row (+ row 1)
        min-col (- col 1)
        max-col (+ col 1)]
    (and
     (or (not= row (:row other)) (not= col (:col other)))
     (<= min-row (:row other) max-row)
     (<= min-col (:col other) max-col))))

(defn- adjacent-cells
  "Returns adjacent cells in the board."
  [{:keys [row col] :as cell} board]
  (for [rd (range -1 2)
        cd (range -1 2)
        :let [adj {:row (+ rd row) :col (+ cd col)}]
        :when (and (or (not= rd 0) (not= cd 0))
                   (contains? board adj))]
    adj))

(defn- adjacent-mine-count
  "Returns how many mines are adjacent to the cell."
  [{:keys [col, row] :as cell} mine-cells]
  (->> mine-cells
       (filter #(cell-adjacent? cell %1))
       count))

(defn- update-game-status
  "Updates the game status based upon the board state."
  [{:keys [board mine-count] :as game}]
  (cond
    ; lost
    (some #(= :exploded %) (vals board))
    (assoc game :status :lost)

    ; won
    (= mine-count
       (->> board vals (filter (fn [v] (#{:hidden :flagged} v))) count))
    (assoc game :status :won)

    ; still playing
    :else
    game))

(defn reveal
  "Reveals a cell"
  [{:keys [mines mine-count mine-generator status board] :as game} cell]
  {:pre [(= :hidden (get board cell))
         (= status :playing)]}
  (cond
    (not= mine-count (count mines))
    (reveal (mine-generator game cell) cell)

    (contains? mines cell)
    (-> game
        (assoc-in [:board cell] :exploded)
        (update-game-status))

    true
    (letfn [(reveal-fn [{:keys [board mines status] :as game} cell]
              (let [state (adjacent-mine-count cell mines)
                    new-game (assoc-in game [:board cell] state)]
                (if (= 0 state)
                  (let [adjacent-cells (adjacent-cells cell (:board new-game))
                        reduce-fn (fn [fn-game fn-cell]
                                    (if (#{:hidden :flagged} (get-in fn-game [:board fn-cell]))
                                      (reveal-fn fn-game fn-cell)
                                      fn-game))]
                    (reduce reduce-fn new-game adjacent-cells))
                  new-game)))]
      (-> game
          (reveal-fn cell)
          (update-game-status)))))

(defn toggle-flag
  "Toggles the flag status on the cell."
  [{:keys [board status] :as game} cell]
  {:pre [(#{:hidden :flagged} (get board cell))
         (= status :playing)]}
  (let [state (get board cell)]
    (case state
      :flagged
      (assoc-in game [:board cell] :hidden)

      :hidden
      (assoc-in game [:board cell] :flagged))))

;; Solver
(defn- mine-count-satisfied?
  "Returns true when a cell's mine count is satisfied by flagged cells."
  [cell board]
  {:pre [(number? (get board cell))]}
  (let [mines (get board cell)
        adjacent-flags (->> board
                            (adjacent-cells cell)
                            (filter #(= :flagged (get board %)))
                            (count))]
    (<= mines adjacent-flags)))

(defn- all-adjacent-need-flag?
  "Returns true when the cell's mine count is satisfied by all adjacent cells being flagged."
  [cell board]
  (let [mines (get board cell)
        potential-flags (->> board
                             (adjacent-cells cell)
                             (filter (fn [cell] (#{:flagged :hidden} (get board cell))))
                             (count))]
    (= mines potential-flags)))

(defn- next-step-for-cell
  "Figures out the next step to take for a cell.  Returns nil if there is no next step."
  [cell board]
  (if (= :hidden (get board cell))
    (let [adjacent-cells (adjacent-cells cell board)]
      (cond
        (some #(all-adjacent-need-flag? % board)
              (filter #(number? (get board %)) adjacent-cells))
        [:flag cell]

        (some #(mine-count-satisfied? % board)
              (filter #(number? (get board %)) adjacent-cells))
        [:reveal cell]))
    nil))

(defn next-step
  "Returns the next step to make for the game."
  [{:keys [status board] :as game}]
  {:pre [(= :playing status)]}
  (first (filter some? (map #(next-step-for-cell % board)
                            (keys board)))))

(defn take-step
  "Takes the next step for the game."
  [game]
  (let [step (next-step game)]
    (case (first step)
      :flag
      (toggle-flag game (second step))

      :reveal
      (reveal game (second step))

      game)))

;; Mine Generators
(defn mine-generator-standard
  "'Standard' minefield generator.

  It ensures the first cell they click on, and all cells adjacent to it, aren't mines.
  That aside, it's completely random."
  [{:keys [board mine-count] :as game} cell]
  (let [cells (into #{} (keys board))
        free-cells (into #{cell} (adjacent-cells cell board))
        candidate-cells (set/difference cells free-cells)
        mines (into #{} (take mine-count (shuffle candidate-cells)))]
    (assoc game :mines mines)))

(defn mine-generator-impossible
  "Generates a minefield that is impossible to win.

  The first cell they click on is always a mine."
  [{:keys [board mine-count] :as game} cell]
  (let [cells (into #{} (keys board))
        candidate-cells (disj cells cell)
        mines (into #{cell} (take (- mine-count 1) (shuffle candidate-cells)))]
    (assoc game :mines mines)))

(def mine-generators
  (array-map
   :standard {:name "Standard" :generator mine-generator-standard}
   :impossible {:name "Impossible" :generator mine-generator-impossible}))

(def mine-generator-default mine-generator-standard)
