import Link from "next/link";

export default function EventCard({ e, href }: { e: any; href?: string }) {
  const target = href || `/events/${e.uniqueEventLink}`;
  return (
    <Link href={target} className="card overflow-hidden transition hover:shadow-md">
      {e.bannerUrl ? (
        <img src={e.bannerUrl} alt={e.title} className="h-40 w-full object-cover" />
      ) : (
        <div className="flex h-40 w-full items-center justify-center bg-gradient-to-br from-indigo-100 to-slate-100 text-slate-400">No banner</div>
      )}
      <div className="p-4">
        <div className="mb-1 flex items-center gap-2">
          <span className="rounded bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">{e.category}</span>
          {e.status && <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-700">{e.status}</span>}
          {e.isSoldOut && <span className="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700">Sold out</span>}
        </div>
        <h3 className="font-semibold">{e.title}</h3>
        <p className="mt-1 text-xs text-slate-500">{e.eventDate} · {e.startTime}</p>
        <p className="text-xs text-slate-500">{e.venue}</p>
        {typeof e.availableCount === "number" && (
          <p className="mt-2 text-xs text-slate-600">{e.availableCount} / {e.totalCapacity} seats available</p>
        )}
      </div>
    </Link>
  );
}
