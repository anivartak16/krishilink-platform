import React, { useEffect, useState } from "react";
import api from "../api/client.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useLanguage } from "../context/LanguageContext.jsx";
import { StatusBadge } from "../components/common/StatusBadge.jsx";

export function MatchesPage() {
  const { user } = useAuth();
  const { t, getLabel } = useLanguage();
  const isFpo = user?.role === "FPO";
  const isBuyer = user?.role === "BUYER";

  const [fpoData, setFpoData] = useState(null);
  const [demands, setDemands] = useState([]);
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingDemandId, setLoadingDemandId] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const endpoint = isBuyer ? "/demands?mine=true" : "/demands";
      const res = await api.get(endpoint);
      setDemands(res.data?.data || []);
    } catch {
      setDemands([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [isFpo, isBuyer]);

  const loadMatches = async (demandId) => {
    setLoadingDemandId(demandId);
    try {
      const x = await api.get("/matches/" + demandId);
      setMatches(x.data?.data || []);
    } catch {
      setMatches([]);
    } finally {
      setLoadingDemandId(null);
    }
  };

  return (
    <section>
      <p className="eyebrow">EXPLAINABLE MATCHING</p>
      <h1>
        {user?.role === "BUYER" ? "Recommended lots" : "Demand-to-lot matching"}
      </h1>
      <p>
        Scores use commodity, quantity, quality, location, price and deadline
        criteria—never a black box.
      </p>

      {loading && <p>Loading demands…</p>}

      {!loading && demands.length === 0 && (
        <p className="subtext">No active demands found for matching.</p>
      )}

      <div className="lots">
        {demands.map((d) => {
          const demandId = d.id || d._id;
          return (
            <article className="lot" key={demandId}>
              <StatusBadge>{d.status}</StatusBadge>
              <h3>
                {d.commodity} · {d.requiredQuantity} kg
              </h3>
              <p>
                Maximum ₹{d.maxPrice}/kg · {d.preferredLocation}
              </p>
              <button
                className="primary"
                onClick={() => loadMatches(demandId)}
                disabled={loadingDemandId === demandId}
              >
                {loadingDemandId === demandId ? "Calculating..." : "Show ranked matches"}
              </button>
            </article>
          );
        })}
      </div>

      {matches.map((m) => (
        <div className="match" key={m.lot?.id || m.lot?._id || m.matchScore}>
          <div className="match-heading">
            <b>{m.matchScore}% fair match</b>
            <span>
              {m.lot?.commodity} · {m.lot?.remainingQuantity} kg · ₹
              {m.lot?.expectedPrice}/kg
            </span>
          </div>
          <div className="match-breakdown">
            {Object.entries(m.breakdown || {}).map(([key, value]) => (
              <span key={key}>
                <b>{value}</b>/ {key}
              </span>
            ))}
          </div>
          <p>{m.reasons?.map((x) => "✓ " + x).join(" · ")}</p>
        </div>
      ))}
    </section>
  );
}

export default MatchesPage;
