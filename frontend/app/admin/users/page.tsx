"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { authApi } from "@/src/lib/api";
export default function Users() {
  const [users,setUsers]=useState<any[]>([]); const [err,setErr]=useState<string|null>(null);
  async function load(){try{setUsers(await authApi.users());}catch(e:any){setErr(e.message);}}
  useEffect(()=>{load();},[]);
  async function deactivate(email:string){ if(!confirm(`Deactivate ${email}?`))return; try{await authApi.deactivateUser(email); load();}catch(e:any){alert(e.message);} }
  return (<div>
    <div className="mb-6 flex items-center justify-between">
      <h1 className="text-2xl font-semibold">Users</h1>
      <Link href="/admin/users/new" className="btn-primary">+ New User</Link>
    </div>
    {err && <p className="text-red-600">{err}</p>}
    <div className="card overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-left"><tr><th className="px-4 py-2">Name</th><th>Email</th><th>Role</th><th>Created</th><th></th></tr></thead>
        <tbody>{users.map(u=>(
          <tr key={u.id} className="border-t"><td className="px-4 py-2 font-medium">{u.name}</td><td>{u.email}</td><td>{u.role}</td><td>{u.createdAt}</td>
            <td className="px-4 py-2"><button onClick={()=>deactivate(u.email)} className="text-red-600 hover:underline">Deactivate</button></td></tr>
        ))}</tbody>
      </table>
    </div>
  </div>);
}
