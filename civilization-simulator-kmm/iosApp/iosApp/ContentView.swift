import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = SimulationViewModel()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Rome Survives to Modern Era")
                        .font(.title2)
                    Text("What if the Western Roman Empire endured through history?")
                        .font(.body)
                        .foregroundColor(.secondary)

                    HStack(spacing: 8) {
                        Button("Run Single") { viewModel.runSingle() }
                            .buttonStyle(.borderedProminent)
                            .disabled(viewModel.isRunning)

                        Button("Monte Carlo (50)") { viewModel.runMonteCarlo() }
                            .buttonStyle(.borderedProminent)
                            .disabled(viewModel.isRunning)
                    }

                    if viewModel.isRunning {
                        ProgressView("Running simulation...")
                    }

                    if let error = viewModel.errorMessage {
                        Text("Error: \(error)")
                            .foregroundColor(.red)
                    }

                    if let result = viewModel.singleResult {
                        ResultCard(title: "Single Run", content: [
                            "Duration: \(viewModel.durationMs) ms",
                            "Population: \(result.finalState.population.population)",
                            "Wealth: \(String(format: "%.0f", result.finalState.economy.wealth))",
                            "Techs: \(result.finalState.technology.unlockedTechs.count)",
                            "Events: \(result.events.count)"
                        ])
                    }

                    if let analysis = viewModel.analysis {
                        ResultCard(title: "Monte Carlo Analysis", content: [
                            "Duration: \(viewModel.durationMs) ms",
                            "Total Runs: \(analysis.totalRuns)",
                            "Avg Population: \(String(format: "%.0f", analysis.avgPopulation))",
                            "Avg Wealth: \(String(format: "%.0f", analysis.avgWealth))",
                            "Avg Techs: \(String(format: "%.1f", analysis.avgTechs))",
                            "Survival Rate: \(String(format: "%.1f", analysis.survivalRate * 100))%"
                        ])
                    }
                }
                .padding()
            }
            .navigationTitle("Civilization Simulator")
        }
    }
}

struct ResultCard: View {
    let title: String
    let content: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.headline)
            ForEach(content, id: \.self) { line in
                Text(line).font(.body)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}
