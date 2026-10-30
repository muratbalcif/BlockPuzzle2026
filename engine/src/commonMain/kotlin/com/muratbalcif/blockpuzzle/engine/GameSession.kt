package com.muratbalcif.blockpuzzle.engine

enum class GameStatus { ACTIVE, PAUSED, FINISHED }
enum class EndReason { NO_MOVES, TIMEOUT_LIMIT }
enum class MoveRejection { FINISHED, PAUSED, INVALID_SLOT, EMPTY_SLOT, INVALID_PLACEMENT }

sealed interface MoveResult {
    val game: GameSession
    data class Placed(override val game: GameSession, val placement: Placement) : MoveResult
    data class Rejected(override val game: GameSession, val reason: MoveRejection) : MoveResult
}

/** Immutable engine state. Only explicit actions advance play or consume time. */
class GameSession private constructor(
    val config: GameConfig,
    val board: Board,
    hand: List<Piece?>,
    private val generator: PieceGenerator,
    val score: Long,
    val timeouts: Int,
    val remainingMillis: Long?,
    private val paused: Boolean,
    val endReason: EndReason?,
) {
    private val slots = hand.toList()
    val hand: List<Piece?> get() = slots.toList()
    val status: GameStatus get() = when {
        endReason != null -> GameStatus.FINISHED
        paused -> GameStatus.PAUSED
        else -> GameStatus.ACTIVE
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        config = config,
        occupiedCells = board.occupiedCells.sortedWith(compareBy({ it.row }, { it.column })),
        hand = slots.map { it?.cells?.sortedWith(compareBy({ it.row }, { it.column })) },
        generatorState = generator.state,
        score = score,
        timeouts = timeouts,
        remainingMillis = remainingMillis,
        endReason = endReason,
    )

    fun place(slot: Int, origin: Cell): MoveResult {
        if (status == GameStatus.FINISHED) return MoveResult.Rejected(this, MoveRejection.FINISHED)
        if (paused) return MoveResult.Rejected(this, MoveRejection.PAUSED)
        if (slot !in slots.indices) return MoveResult.Rejected(this, MoveRejection.INVALID_SLOT)
        val piece = slots[slot] ?: return MoveResult.Rejected(this, MoveRejection.EMPTY_SLOT)
        val placement =
            board.place(piece, origin)
                ?: return MoveResult.Rejected(this, MoveRejection.INVALID_PLACEMENT)
        var nextHand = slots.mapIndexed { index, available ->
            if (index ==
                slot
            ) {
                null
            } else {
                available
            }
        }
        var nextGenerator = generator
        if (nextHand.all { it == null }) {
            val generated = generator.nextHand(config)
            nextHand = generated.pieces
            nextGenerator = generated.generator
        }
        val noMoves = nextHand.filterNotNull().none { placement.board.canFit(it) }
        val game = transition(
            board = placement.board,
            hand = nextHand,
            generator = nextGenerator,
            score = if (score >
                Long.MAX_VALUE - placement.points
            ) {
                Long.MAX_VALUE
            } else {
                score + placement.points
            },
            remainingMillis = if (noMoves &&
                config.tempo.moveLimitMillis != null
            ) {
                0L
            } else {
                config.tempo.moveLimitMillis
            },
            endReason = if (noMoves) EndReason.NO_MOVES else null,
        )
        return MoveResult.Placed(game, placement)
    }

    /** Pass only elapsed foreground, unpaused time from a monotonic platform clock. */
    fun advanceTime(elapsedMillis: Long): GameSession {
        require(elapsedMillis >= 0) { "Elapsed time cannot be negative" }
        if (status != GameStatus.ACTIVE || elapsedMillis == 0L) return this
        val remaining = remainingMillis ?: return this
        if (elapsedMillis <
            remaining
        ) {
            return transition(remainingMillis = remaining - elapsedMillis)
        }
        val limit = checkNotNull(config.tempo.moveLimitMillis)
        val afterFirstTimeout = elapsedMillis - remaining
        val strikes = minOf(
            1L + afterFirstTimeout / limit,
            (MAX_TIMEOUTS - timeouts).toLong(),
        ).toInt()
        val nextTimeouts = timeouts + strikes
        val finished = nextTimeouts == MAX_TIMEOUTS
        return transition(
            score = (score - TIMEOUT_PENALTY * strikes).coerceAtLeast(0L),
            timeouts = nextTimeouts,
            remainingMillis = if (finished) 0L else limit - afterFirstTimeout % limit,
            endReason = if (finished) EndReason.TIMEOUT_LIMIT else null,
        )
    }

    fun pause(): GameSession = if (status == GameStatus.ACTIVE) transition(paused = true) else this

    fun resume(): GameSession =
        if (status == GameStatus.PAUSED) transition(paused = false) else this

    private fun transition(
        board: Board = this.board,
        hand: List<Piece?> = slots,
        generator: PieceGenerator = this.generator,
        score: Long = this.score,
        timeouts: Int = this.timeouts,
        remainingMillis: Long? = this.remainingMillis,
        paused: Boolean = this.paused,
        endReason: EndReason? = this.endReason,
    ): GameSession = GameSession(
        config,
        board,
        hand,
        generator,
        score,
        timeouts,
        remainingMillis,
        paused,
        endReason,
    )

    companion object {
        fun restore(snapshot: GameSnapshot): GameSession {
            require(snapshot.rulesVersion == 1) { "Unsupported rules version" }
            require(snapshot.score >= 0 && snapshot.timeouts in 0..MAX_TIMEOUTS)
            require(snapshot.occupiedCells.size == snapshot.occupiedCells.toSet().size)
            val board = Board.fromCells(snapshot.config.boardSize, snapshot.occupiedCells)
            require(snapshot.hand.size == 3 && snapshot.hand.any { it != null })
            val hand = snapshot.hand.map { cells -> cells?.let { Piece(it) } }
            require(
                hand.filterNotNull().all {
                    it in PieceCatalog.all && it.width <= board.size && it.height <= board.size
                },
            ) { "Invalid available piece" }
            val generator = PieceGenerator.fromState(snapshot.generatorState)
            val anyFit = hand.filterNotNull().any { board.canFit(it) }
            val limit = snapshot.config.tempo.moveLimitMillis
            if (limit == null) {
                require(snapshot.remainingMillis == null && snapshot.timeouts == 0)
                require(snapshot.endReason != EndReason.TIMEOUT_LIMIT)
            } else {
                when (snapshot.endReason) {
                    null -> require(
                        snapshot.timeouts < MAX_TIMEOUTS && snapshot.remainingMillis in 1L..limit,
                    )
                    EndReason.NO_MOVES -> require(
                        snapshot.timeouts < MAX_TIMEOUTS && snapshot.remainingMillis == 0L,
                    )
                    EndReason.TIMEOUT_LIMIT -> require(
                        snapshot.timeouts == MAX_TIMEOUTS && snapshot.remainingMillis == 0L,
                    )
                }
            }
            when (snapshot.endReason) {
                null -> require(anyFit) { "Unfinished game must have a legal move" }
                EndReason.NO_MOVES -> require(!anyFit) { "No-moves result disagrees with board" }
                EndReason.TIMEOUT_LIMIT -> Unit
            }
            return GameSession(
                snapshot.config, board, hand, generator, snapshot.score, snapshot.timeouts,
                snapshot.remainingMillis, paused = true, endReason = snapshot.endReason,
            )
        }

        private const val MAX_TIMEOUTS = 3
        private const val TIMEOUT_PENALTY = 10L

        /**
         * Supports deterministic custom-board scenarios, not saved-game restoration.
         */
        fun start(
            config: GameConfig = GameConfig(),
            seed: Long,
            initialBoard: Board = Board.empty(config.boardSize),
        ): GameSession {
            require(initialBoard.size == config.boardSize) {
                "Board size differs from configuration"
            }
            val generated = PieceGenerator.seeded(seed).nextHand(config)
            val noMoves = generated.pieces.none { initialBoard.canFit(it) }
            return GameSession(
                config = config,
                board = initialBoard,
                hand = generated.pieces,
                generator = generated.generator,
                score = 0L,
                timeouts = 0,
                remainingMillis = if (noMoves &&
                    config.tempo.moveLimitMillis != null
                ) {
                    0L
                } else {
                    config.tempo.moveLimitMillis
                },
                paused = false,
                endReason = if (noMoves) EndReason.NO_MOVES else null,
            )
        }
    }
}
