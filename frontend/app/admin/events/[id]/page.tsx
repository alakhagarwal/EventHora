"use client";
import { useParams } from "next/navigation";
import EventForm from "@/src/components/EventForm";
export default function EditEvent() {
  const { id } = useParams<{ id: string }>();
  return (<div><h1 className="mb-6 text-2xl font-semibold">Edit Event</h1><EventForm eventId={id as string} /></div>);
}
