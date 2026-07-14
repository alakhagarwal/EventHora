"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/src/lib/api";
export default function NewUser() {
  const r = useRouter();
  const [f,setF]=useState({name:"",email:"",password:"",role:"STAFF"});
  const [err,setErr]=useState<string|null>(null); const [busy,setBusy]=useState(false);
  async function submit(e:React.FormEvent){e.preventDefault();setErr(null);setBusy(true);
    try{await authApi.createUser(f); r.push("/admin/users");}catch(x:any){setErr(x.message);}finally{setBusy(false);}
  }
  return (<div className="mx-auto max-w-md">
    <h1 className="mb-6 text-2xl font-semibold">Create User</h1>
    <form onSubmit={submit} className="card space-y-4 p-6">
      {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700">{err}</p>}
      <div><label className="label">Name</label><input className="input" value={f.name} onChange={e=>setF({...f,name:e.target.value})} required /></div>
      <div><label className="label">Email</label><input type="email" className="input" value={f.email} onChange={e=>setF({...f,email:e.target.value})} required /></div>
      <div><label className="label">Password</label><input type="password" className="input" value={f.password} onChange={e=>setF({...f,password:e.target.value})} required /></div>
      <div><label className="label">Role</label>
        <select className="input" value={f.role} onChange={e=>setF({...f,role:e.target.value})}><option>ADMIN</option><option>STAFF</option></select>
      </div>
      <button className="btn-primary w-full" disabled={busy}>{busy?"Creating…":"Create user"}</button>
    </form>
  </div>);
}
