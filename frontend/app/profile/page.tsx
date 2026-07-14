"use client";
import { useEffect, useState } from "react";
import { authApi } from "@/src/lib/api";
export default function Profile() {
  const [me,setMe]=useState<any>(null); const [err,setErr]=useState<string|null>(null);
  useEffect(()=>{authApi.me().then(setMe).catch(e=>setErr(e.message));},[]);
  if (err) return <p className="text-red-600">{err}</p>;
  if (!me) return <p>Loading…</p>;
  return (<div className="mx-auto max-w-md card p-6">
    <h1 className="text-2xl font-semibold">My Profile</h1>
    <dl className="mt-4 space-y-2 text-sm">
      <div><dt className="text-slate-500">Name</dt><dd className="font-medium">{me.name}</dd></div>
      <div><dt className="text-slate-500">Email</dt><dd>{me.email}</dd></div>
      <div><dt className="text-slate-500">Role</dt><dd>{me.role}</dd></div>
      {me.createdAt && <div><dt className="text-slate-500">Created</dt><dd>{me.createdAt}</dd></div>}
    </dl>
  </div>);
}
