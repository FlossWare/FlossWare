import Foundation
import shared

@MainActor
class SimulationViewModel: ObservableObject {
    @Published var isRunning = false
    @Published var singleResult: SimulationResult? = nil
    @Published var analysis: MonteCarloRunner.MonteCarloAnalysis? = nil
    @Published var durationMs: Int64 = 0
    @Published var errorMessage: String? = nil

    func runSingle() {
        isRunning = true
        errorMessage = nil
        Task {
            do {
                let scenario = RomeEnduresScenario.shared.create()
                let start = DispatchTime.now()
                let engine = SimulationEngine(scenario: scenario, baseSeed: scenario.simulationRules.baseRandomSeed)
                let result = engine.run(runIndex: 0)
                let end = DispatchTime.now()
                let nanos = end.uptimeNanoseconds - start.uptimeNanoseconds
                self.durationMs = Int64(nanos / 1_000_000)
                self.singleResult = result
                self.isRunning = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isRunning = false
            }
        }
    }

    func runMonteCarlo() {
        isRunning = true
        errorMessage = nil
        Task {
            do {
                let scenario = RomeEnduresScenario.shared.create()
                let start = DispatchTime.now()
                let runner = MonteCarloRunner(scenario: scenario)
                let results = try await runner.runAll()
                let end = DispatchTime.now()
                let nanos = end.uptimeNanoseconds - start.uptimeNanoseconds
                self.durationMs = Int64(nanos / 1_000_000)
                self.analysis = MonteCarloRunner.companion.analyze(results: results)
                self.isRunning = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isRunning = false
            }
        }
    }
}
