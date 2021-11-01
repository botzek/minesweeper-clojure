(ns minesweeper.game)

(defn- cell-adjacent? [{:keys [row col] :as cell} other]
  (let [min-row (- row 1)
        max-row (+ row 1)
        min-col (- col 1)
        max-col (+ col 1)]
    (and
     (or (not= row (:row other)) (not= col (:col other)))
     (<= min-row (:row other) max-row)
     (<= min-col (:col other) max-col))))

(defn- adjacent-mine-count [{:keys [col, row] :as cell} mine-cells]
  (->> mine-cells
       (filter #(cell-adjacent? cell %1))
       count))

(defn reveal [{:keys [mines] :as game} cell]
  {:pre [(= :hidden (get-in game [:board cell]))]}
  (let [state (if (contains? mines cell)
                :mine
                (adjacent-mine-count cell mines))
        new-game (assoc-in game [:board cell] state)]
    (case state
      :mine
      new-game

      0
      (let [adjacent-cells (filter #(cell-adjacent? % cell)
                                   (-> new-game :board keys))
            reveal-fn (fn [fn-game fn-cell]
                        (if (= :hidden (-> fn-game :board (get fn-cell)))
                          (reveal fn-game fn-cell)
                          fn-game))]
        (reduce reveal-fn new-game adjacent-cells))

      ; else
      new-game)))

(defn toggle-flag [game cell]
  {:pre [(some #(= % (get-in game [:board cell])) [:hidden :flagged])]}
  (let [state (get-in game [:board cell])]
    (case state
      :flagged
      (assoc-in game [:board cell] :hidden)

      :hidden
      (assoc-in game [:board cell] :flagged))))

(defn make-game
  ([difficulty]
   (case difficulty
     :trivial (make-game 9 9 5)
     :beginner (make-game 9 9 10)
     :intermediate (make-game 16 16 40)
     :expert (make-game 16 30 99)))
  ([rows cols mine-count]
  (let [cells (for [r (range rows)
                    c (range cols)]
                {:row r :col c})
        board (zipmap cells (repeat :hidden))
        mines (into #{} (take mine-count (shuffle cells)))]
    {:mines mines
     :rows rows
     :cols cols
     :board board})))
