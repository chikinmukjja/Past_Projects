#ifndef USERPROG_SYSCALL_H
#define USERPROG_SYSCALL_H
struct vm_entry* check_address(void *addr);
void munmap(int mapping);
void syscall_init (void);
void exit(int status);
#endif /* userprog/syscall.h */
